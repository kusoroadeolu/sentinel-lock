package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.SyncRegistry.LeaseResult.AlreadyLeased;
import io.github.kusoroadeolu.sentinellock.SyncRegistry.LeaseResult.Success;
import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;
import io.github.kusoroadeolu.sentinellock.entities.LeaseResponse.CompletedLeaseResponse;
import io.github.kusoroadeolu.sentinellock.entities.LeaseResponse.FailedLeaseResponse;
import io.github.kusoroadeolu.sentinellock.entities.LeaseResponse.WaitingLeaseResponse;
import io.github.kusoroadeolu.sentinellock.exceptions.LeaseConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static io.github.kusoroadeolu.sentinellock.SyncRegistry.LeaseResult.AlreadyLeased.LEASED;
import static io.github.kusoroadeolu.sentinellock.SyncRegistry.LeaseResult.Conflict.CONFLICT;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendLeasePrefix;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncRegistry {
    private final RequestQueue requestQueue;
    private final RedisTemplate<String, LeaseState> lockStateTemplate;
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final SentinelLockConfigProperties configProperties;

    public @NonNull LeaseResponse ask(@NonNull PendingRequest request) { //Users should probably wait until they acquire the lock, so we need a way to make them wait
        final var future = new CompletableFuture<CompletedLeaseResponse>();
        final var res = this.tryAcquireOrQueue(request, future);
        log.info("Result: {}", res);
        if (res instanceof Success(var clr)) return clr;
        else if (res instanceof AlreadyLeased) return new WaitingLeaseResponse(future);
        else return FailedLeaseResponse.FAILED;
    }

    @Retryable(includes = LeaseConflictException.class, jitter = 2)
    public LeaseResult tryAcquireOrQueue(@NonNull PendingRequest request, @NonNull CompletableFuture<CompletedLeaseResponse> future){
        final var syncKey = request.syncKey();
        final var key = syncKey.key();
        final var clientId = request.id();
        final var leaseDuration = request.requestedLeaseDuration();

        final var leaseResult = this.createLease(syncKey, leaseDuration, clientId);
         switch (leaseResult){
            case AlreadyLeased _ -> this.requestQueue.offer(request, future);
            //TODO handle case in which offer returns false
            case Success s -> {
                log.info("Successfully leased key: {} to client: {}", key, clientId);
                future.complete(s.lrp());
            }
            case LeaseResult.Conflict _ -> throw new LeaseConflictException();
            case LeaseResult.TransactionError _ -> {} //TODO handle transaction errors
        }

        return leaseResult;
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
                        return new Success(new CompletedLeaseResponse(syncKey, nextToken));
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

        record Success(CompletedLeaseResponse lrp) implements LeaseResult{} //represents if this sync is unleased
    }
}
