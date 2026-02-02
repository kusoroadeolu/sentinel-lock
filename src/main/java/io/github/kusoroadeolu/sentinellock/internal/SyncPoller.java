package io.github.kusoroadeolu.sentinellock.internal;

import io.github.kusoroadeolu.sentinellock.entities.LeaseState;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.Synchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

import static io.github.kusoroadeolu.sentinellock.utils.Constants.SYNC_PREFIX;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncPoller {
    private final ExecutorService pollingExecutor;
    private final RedisTemplate<String, LeaseState> leaseStateTemplate;
    private final RedisTemplate<String, Synchronizer> synchronizerTemplate;
    private final RequestQueue requestQueue;  //TODO add request key to redis on redis shutdown to prevent loss of data
    private final LeaseRegistry registry;

    @Async
    @Scheduled(fixedRateString = "${sentinel-lock.poll-rate-ms}")
    public void poll(){
       final var wildcard = SYNC_PREFIX + "*";

        final var options = KeyScanOptions
                .scanOptions()
                .count(100)
                .match(wildcard)
                .build();

        long keyProcessed = 0; //This will be for metrics
        try (var cursor = this.synchronizerTemplate.scan(options)) {
            while (cursor.hasNext()) {
                var key = cursor.next();
                var lockState = this.leaseStateTemplate.opsForValue().get(key);
                // I can't expire the key here and delegate it to the redis listener to prevent a higher possibility of retries
                // if someone wants to acquire a lease because expiry acts as modifying a key lol
                if (!isNull(lockState) && !lockState.isExpired()) return;
                this.pollingExecutor.submit(() -> {
                    var optional = requestQueue.poll(new SyncKey(key));
                    optional.ifPresent(qpr -> {
                        this.pollingExecutor.submit(() -> registry.ask(qpr.request(), qpr.future()));
                    });
                });
                ++keyProcessed;
            }
        }catch (Exception e){
            log.info("Failed to poll for expired keys due to ");
        }

    }
}
