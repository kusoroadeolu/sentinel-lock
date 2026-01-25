package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.entities.Lease;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.github.kusoroadeolu.sentinellock.utils.Constants.LEASE_PREFIX;
import static io.github.kusoroadeolu.sentinellock.utils.Constants.SYNC_PREFIX;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisExpiryHandler {
    //FLOW: Ask -> Try acquire -> Fail -> Queue -> Trigger event -> Poll -> Retry asking till lease acquire
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final RedisTemplate<String, Lease> leaseTemplate;
    private final RedisTemplate<String, Long> fencingTokenTemplate;
    private final RequestQueue requestQueue;
    private final SyncRegistry registry;

    @EventListener
    public void handleLeaseExpiry(RedisKeyExpiredEvent<?> event) throws ExecutionException, InterruptedException, TimeoutException {
        final var bytes = event.getId();
        if (bytes.length == 0) return;
        final var key = new String(bytes);
        if (key.startsWith(SYNC_PREFIX)){
            log.info("Sync expiry triggered for key {}", key);
            this.synchronizerTemplate.delete(key);
            this.leaseTemplate.delete(key);
            this.fencingTokenTemplate.delete(key);
        }else if (key.startsWith(LEASE_PREFIX)){
            log.info("Lease expiry triggered for key {}", key);
            var opt = this.requestQueue.poll(new SyncKey(key));
            if (opt.isPresent()){
                var p = opt.get();
                this.requestQueue.remove(p);
                this.registry.tryAcquireOrQueue(p.request(), p.future());
            }
        }
    }
}
