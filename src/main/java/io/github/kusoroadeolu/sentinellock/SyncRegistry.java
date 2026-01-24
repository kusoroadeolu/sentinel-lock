package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
//Quick thought, maybe I should add a map of SyncRegistries over a wrapper class, each registry runs on a single virtual thread, serializes access and prevents any odd race condition
public class SyncRegistry {
    private final ClientMap clientMap;
    private final RedisTemplate<ClientId, Synchronizer> synchronizerTemplate;
    private final RedisTemplate<ClientId, Lease> leaseTemplate;
    private final RedisTemplate<ClientId, Long> fencingTokenTemplate;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SentinelLockConfigProperties configProperties;

    public LeaseResponse ask(@NonNull PendingRequest request){
        final var id = request.id();
        final var syncTtl = this.configProperties.syncIdleTtl();
        boolean isSyncAvail = this.synchronizerTemplate.opsForValue().setIfAbsent(id, new Synchronizer(id), Duration.ofMillis(syncTtl));
        var sync = this.synchronizerTemplate.opsForValue().get(id);
    }
}
