package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.AlreadyLeased;
import io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.Success;
import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;
import io.github.kusoroadeolu.sentinellock.entities.Lease.CompleteLease;
import io.github.kusoroadeolu.sentinellock.exceptions.LeaseConflictException;
import io.github.kusoroadeolu.sentinellock.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static io.github.kusoroadeolu.sentinellock.LeaseRegistry.LeaseResult.AlreadyLeased.LEASED;
import static io.github.kusoroadeolu.sentinellock.entities.CompletableLease.Status.FAILED;
import static io.github.kusoroadeolu.sentinellock.entities.Lease.FailedLease;
import static io.github.kusoroadeolu.sentinellock.entities.Lease.FailedLease.Cause.*;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendLeasePrefix;
import static io.github.kusoroadeolu.sentinellock.utils.Utils.appendSyncPrefix;
import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaseRegistry {
    private final RequestQueue requestQueue;
    private final RedisTemplate<String, LeaseState> leaseStateTemplate;
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final SentinelLockConfigProperties configProperties;
    private final RetryTemplate leaseRetryTemplate;

    public void ask(@NonNull PendingRequest request, CompletableLease future) { //Users should probably wait until they acquire the lock, so we need a way to make them wait
        this.tryAcquireOrQueue(request, future);
    }

    public ReleaseResult release(@NonNull Lease lease){
        if (lease instanceof CompleteLease c) return this.releaseLease(c);
        return ReleaseResult.FAILED;
    }

    public void tryAcquireOrQueue(@NonNull PendingRequest request, @NonNull CompletableLease future){
        final var syncKey = request.syncKey();
        final var key = syncKey.key();
        final var clientId = request.id();
        final var leaseDuration = request.requestedLeaseDuration();

        if (leaseDuration >= this.configProperties.maxLeaseRequestDuration()){
            future.completeExceptionally(new FailedLease(INVALID_LEASE_DURATION), FAILED);
        }
        final var leaseResult = this.createLease(syncKey, leaseDuration, clientId);
         switch (leaseResult){
            case AlreadyLeased _ -> {
                 boolean notFull = this.requestQueue.offer(request, future);
                 if (!notFull) {
                     future.completeExceptionally(new FailedLease(QUEUE_FULL), FAILED);
                     log.info("Failed to add client: {} requesting for sync key: {} to queue", clientId, syncKey);
                 }else {
                     log.info("Added client: {} requesting for sync key: {} to queue", clientId, syncKey);
                 }
            }
            case Success s -> {
                future.complete(s.lrp());
                log.info("Successfully leased key: {} to client: {}", key, clientId);
            }
            case LeaseResult.TransactionError _ -> future.completeExceptionally(new FailedLease(ERR), FAILED);
        }
    }

    LeaseResult createLease(SyncKey key, long leaseDuration, ClientId id) {
        final var rawKey = key.key();
        final var redisOps = this.leaseStateTemplate.opsForValue().getOperations();
        final var syncKey = appendSyncPrefix(rawKey);
        final var leaseKey = appendLeasePrefix(rawKey);
        final var syncTtl = this.configProperties.syncIdleTtl();

        //If another client modifies in between us watching and our call to exec, we can assume another client has acquired the lease
        try {
           return this.leaseRetryTemplate.execute(() ->
                 redisOps.execute(
                        new LeaseTransactionCallback(leaseKey, syncKey, key, id, syncTtl ,leaseDuration, rawKey)
                )
            );
        } catch (RetryException e){
            return new LeaseResult.TransactionError(e);
        }

    }

    ReleaseResult releaseLease(CompleteLease lease){
        final var redisOps = this.leaseStateTemplate.opsForValue().getOperations();
        try {
            return this.leaseRetryTemplate.execute(() ->
                    redisOps.execute(new ReleaseTransactionCallback(lease))
            );
        } catch (RetryException e){
            return ReleaseResult.FAILED;
        }
    }


    Synchronizer createSynchronizerIfAbsent(String key, long syncTtl){
        final var sync = this.synchronizerTemplate.opsForValue().get(key);
        if (isNull(sync)) {
            final var newSync = new Synchronizer(0L);
            final var isAbsent = this.synchronizerTemplate.opsForValue().setIfAbsent(key, newSync, Duration.ofMillis(syncTtl));
            if (isAbsent) return newSync;
        }

        return sync;
    }

    @RequiredArgsConstructor
    private final class LeaseTransactionCallback implements SessionCallback<LeaseResult> {
        private final String leaseKey;
        private final String syncKey;
        private final SyncKey key;
        private final ClientId id;
        private final long syncTtl;
        private final long leaseDuration;
        private final String rawKey;

        @SuppressWarnings("unchecked")
        public LeaseResult execute(@NonNull RedisOperations ops) throws DataAccessException {
            ops.watch(leaseKey);
            final var currState = (LeaseState) ops.opsForValue().get(leaseKey);
            if (!isNull(currState) && !currState.isExpired()) {
                log.info("Acquired at: {}, Expires at: {}", currState.acquiredAt(), currState.expiresAt());
                ops.unwatch();
                return LEASED;
            }
            final var sync = createSynchronizerIfAbsent(syncKey, configProperties.syncIdleTtl());
            final long nextToken = sync.currentFencingToken() + 1; //sync can never be null
            ops.multi();
            try {

                final var now = Instant.now();
                final var lockState = new LeaseState(key, id, nextToken, now, now.plusMillis(leaseDuration), leaseDuration);
                ops.opsForValue().set(leaseKey, lockState, Duration.ofMillis(leaseDuration));
                final var res = ops.exec();
                if (res.isEmpty()) {
                    log.info("Lease acquire transaction for client: {} failed due to a race condition", id);
                    return LEASED;
                } else {
                    synchronizerTemplate.opsForValue().set(syncKey, new Synchronizer(nextToken), Duration.ofMillis(syncTtl));
                    return new Success(new CompleteLease(key, id ,nextToken));
                }
            }catch (DataAccessException e){
                ops.discard();
                log.error("An error occurred while trying to a perform redis transaction to acquire a lease for key: {}", rawKey, e);
                throw new LeaseConflictException();
            }
        }
    }

    private record ReleaseTransactionCallback(@NonNull CompleteLease lease) implements SessionCallback<ReleaseResult> {
            @SuppressWarnings("unchecked")
            public ReleaseResult execute(@NonNull RedisOperations ops) throws DataAccessException {
                final SyncKey rawKey = this.lease.key();
                var leaseKey = appendLeasePrefix(rawKey.key());

                ops.watch(leaseKey);
                final var currState = (LeaseState) ops.opsForValue().get(leaseKey);
                /*
                * Curr state  == null
                * Curr state current holder != lease's client id
                * Curr state fencing token != lease fencing token
                * Curr state is expired
                * */
                if (isNull(currState) || !currState.currentHolder().equals(lease.id())
                        || lease.fencingToken() != currState.fencingToken() || currState.isExpired()) {
                    log.info("Failed to release lease: {} because {} isn't the current holder.", leaseKey, lease.id());
                    ops.unwatch();
                    return ReleaseResult.FAILED;
                }

                ops.multi();
                try {
                    ops.expire(leaseKey, Duration.ofMillis(0L));
                    final var res = ops.exec();
                    if (res.isEmpty()) {
                        log.info("Lease release transaction for client: {} failed due to a race condition", lease.id());
                        return ReleaseResult.FAILED;
                    }
                    return ReleaseResult.SUCCESS;
                } catch (DataAccessException e) {
                    ops.discard();
                    log.error("An error occurred while trying to a perform redis transaction to release a lease for key: {}", rawKey, e);
                    throw new LeaseConflictException();
                }
            }
        }



    public sealed interface LeaseResult permits AlreadyLeased, Success, LeaseResult.TransactionError {
        enum AlreadyLeased implements LeaseResult{  //represents if this sync is currently leased
            LEASED
        }

        record TransactionError(Exception e) implements LeaseResult{}

        record Success(Lease lrp) implements LeaseResult{} //represents if this sync is unleased
    }

    public enum ReleaseResult{
        SUCCESS, FAILED
    }
}
