package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer.LeaseResult.AlreadyLeased;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer.LeaseResult.Success;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncRegistry {
    private final RequestQueue requestQueue;
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final RedisTemplate<String, Lease> leaseTemplate;
    private final RedisTemplate<String, Long> fencingTokenTemplate;
    private final SentinelLockConfigProperties configProperties;

    public LeaseResponse ask(@NonNull PendingRequest request){ //Users should probably wait until they acquire the lock, so we need a way to make them wait
        final var syncKey = request.syncKey();
        final var key = syncKey.key();

        final var syncTtl = this.configProperties.syncIdleTtl();
        final var syncOps = this.synchronizerTemplate.opsForValue();
        final var sync = this.getSynchronizer(syncKey, syncTtl, syncOps);

        final var leaseDur = request.requestedLeaseDuration();
        //TODO check if the lease duration is longer than the sync duration

        final var leaseResult = requireNonNull(sync)
                .newLease(leaseDur, request.id());

        switch (leaseResult){
            case AlreadyLeased _ -> this.requestQueue.offer(request); //TODO handle case in which offer returns false
            case Success s -> {
                var lease = s.lease();
                lease.setFencingToken(this.fencingTokenTemplate.opsForValue().increment(key));
                this.leaseTemplate.opsForValue().set(key, lease);
                this.leaseTemplate.expire(key, Duration.ofMillis(leaseDur)); //TODO handle when a lease expires
            }
        }

    }


    Synchronizer getSynchronizer(SyncKey syncKey, long syncTtl,ValueOperations<String, Synchronizer> syncOps){
        final var key = syncKey.key();
        var sync = syncOps.get(key);

        if (sync == null) {
            var newSync = new Synchronizer(syncKey);
            if (syncOps.setIfAbsent(key, newSync, Duration.ofMillis(syncTtl))) {
                sync = newSync;
            } else {
                sync = syncOps.get(key);
            }
        }

        this.synchronizerTemplate.expire(key, Duration.ofMillis(syncTtl));
        return sync;
    }
}
