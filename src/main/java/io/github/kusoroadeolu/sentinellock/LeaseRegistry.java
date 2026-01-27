package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.AlreadyLeased;
import io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.Success;
import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;
import io.github.kusoroadeolu.sentinellock.entities.Lease.CompleteLease;
import io.github.kusoroadeolu.sentinellock.exceptions.LeaseConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.AlreadyLeased.LEASED;
import static io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.Conflict.CONFLICT;
import static io.github.kusoroadeolu.sentinellock.entities.CompletableLease.Status.*;
import static io.github.kusoroadeolu.sentinellock.entities.Lease.*;
import static io.github.kusoroadeolu.sentinellock.entities.Lease.FailedLease.Cause.*;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendLeasePrefix;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaseRegistry {
    private final RequestQueue requestQueue;
    private final RedisTemplate<String, LeaseState> lockStateTemplate;
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final SentinelLockConfigProperties configProperties;

    public void ask(@NonNull PendingRequest request, CompletableLease future) { //Users should probably wait until they acquire the lock, so we need a way to make them wait
        this.tryAcquireOrQueue(request, future);
    }

    @Retryable(includes = LeaseConflictException.class, jitter = 2)
    public void tryAcquireOrQueue(@NonNull PendingRequest request, @NonNull CompletableLease future){
        final var syncKey = request.syncKey();
        final var key = syncKey.key();
        final var clientId = request.id();
        final var leaseDuration = request.requestedLeaseDuration();

        final var leaseResult = this.createLease(syncKey, leaseDuration, clientId);
         switch (leaseResult){
            case AlreadyLeased _ -> {
                 boolean notFull = this.requestQueue.offer(request, future);
                 if (!notFull) future.completeExceptionally(new FailedLease(QUEUE_FULL), FAILED);
            }
            case Success s -> {
                log.info("Successfully leased key: {} to client: {}", key, clientId);
                future.complete(s.lrp());
            }
            case LeaseResult.Conflict _ -> throw new LeaseConflictException();
            case LeaseResult.TransactionError _ -> future.completeExceptionally(new FailedLease(ERR), FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    LeaseResult createLease(SyncKey syncKey, long leaseDuration, ClientId id) {
        final var rawKey = syncKey.key();
        final var redisOps = this.lockStateTemplate.opsForValue().getOperations();
        final var appSyncKey = appendSyncPrefix(rawKey);
        final var appLsKey = appendLeasePrefix(rawKey);

        return redisOps.execute(new SessionCallback<>() {
            public LeaseResult execute(@NonNull RedisOperations ops) throws DataAccessException {
                ops.watch(appLsKey);
                final var currState = (LeaseState) ops.opsForValue().get(appLsKey);


                if (currState != null && !currState.isExpired()) {
                    log.info("Acquired at: {}, Expires at: {}", currState.acquiredAt(), currState.expiresAt());
                    ops.unwatch();
                    return LEASED;
                }

                final var sync = createSynchronizerIfAbsent(appSyncKey, configProperties.syncIdleTtl());
                final var nextToken = sync.leaseCount() + 1; //sync can never be null
                ops.multi();
                try {
                    final var now = Instant.now();
                    final var lockState = new LeaseState(syncKey, id, nextToken, now, now.plusMillis(leaseDuration), leaseDuration);
                    ops.expire(appLsKey, Duration.ofMillis(leaseDuration));
                    ops.opsForValue().set(appLsKey, lockState, Duration.ofMillis(leaseDuration));

                    final var res = ops.exec();
                    if (res.isEmpty()) {
                        ops.discard();
                        log.info("Res is empty for client: {}", id);
                        return CONFLICT;
                    } else {
                        synchronizerTemplate.opsForValue().set(appSyncKey, new Synchronizer(nextToken));
                        return new Success(new CompleteLease(syncKey, nextToken));
                    }
                }catch (DataAccessException e){
                    ops.discard();
                    log.error("An error occurred while trying to perform redis transaction for key: {}", rawKey, e);
                    return new LeaseResult.TransactionError(e);
                }
            }
        });
    }

    Synchronizer createSynchronizerIfAbsent(String key, long syncTtl){
        var sync = this.synchronizerTemplate.opsForValue().get(key);
        log.info("Sync: {} Key: {}", sync, key);

        if (sync == null) {
            final var newSync = new Synchronizer(0L);
            final var isAbsent = this.synchronizerTemplate.opsForValue().setIfAbsent(key, newSync, Duration.ofMillis(syncTtl));
            if (isAbsent) return newSync;
        }

        log.info("Set sync: {}", sync);

        this.synchronizerTemplate.expire(key, Duration.ofMillis(syncTtl));
        return sync;
    }

    public sealed interface LeaseResult permits AlreadyLeased, Success, LeaseResult.TransactionError, LeaseResult.Conflict {
        enum AlreadyLeased implements LeaseResult{  //represents if this sync is currently leased
            LEASED
        }

        enum Conflict implements LeaseResult{
            CONFLICT
        }

        record TransactionError(Exception e) implements LeaseResult{}

        record Success(Lease lrp) implements LeaseResult{} //represents if this sync is unleased
    }
}
