package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Thread.ofVirtual;

@Configuration
@RequiredArgsConstructor
public class MiscConfig {

    @Bean
    public Map<SyncKey, BlockingQueue<PendingRequest>> map(){
        return new ConcurrentHashMap<>();
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService(){
        return Executors.newScheduledThreadPool(50, ofVirtual().factory());
    }
}
