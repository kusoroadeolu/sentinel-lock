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
import java.util.concurrent.ScheduledExecutorService;

import static java.util.Objects.requireNonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncRegistry {
    private final ClientMap clientMap;
    private final RedisTemplate<SyncKey, Synchronizer> synchronizerTemplate;
    private final RedisTemplate<SyncKey, Lease> leaseTemplate;
    private final RedisTemplate<SyncKey, Long> fencingTokenTemplate;
    //private final ScheduledExecutorService scheduledExecutorService;
    private final SentinelLockConfigProperties configProperties;

    public LeaseResponse ask(@NonNull PendingRequest request){ //Users should probably wait until they acquire the lock, so we need a way to make them wait
        final var key = request.syncKey();
        final var syncTtl = this.configProperties.syncIdleTtl();
        final var syncOps = this.synchronizerTemplate.opsForValue();
        final var sync = this.getSynchronizer(key, syncTtl, syncOps);

        long leaseDur = request.requestedLeaseDuration();
        //TODO check if the lease duration is longer than the sync duration

        var leaseResult = requireNonNull(sync)
                .newLease(leaseDur, request.id());

        switch (leaseResult){
            case AlreadyLeased _ -> this.clientMap.offer(request); //TODO handle case in which offer returns false
            case Success s -> {
                var lease = s.lease();
                lease.setFencingToken(this.fencingTokenTemplate.opsForValue().increment(key));
                this.leaseTemplate.opsForValue().set(key, lease);
                this.leaseTemplate.expire(key, Duration.ofMillis(leaseDur)); //TODO handle when a lease expires
            }
        }


    }


    Synchronizer getSynchronizer(SyncKey key, long syncTtl,ValueOperations<SyncKey, Synchronizer> syncOps){
        var sync = syncOps.get(key);

        if (sync == null) {
            var newSync = new Synchronizer(key);
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
