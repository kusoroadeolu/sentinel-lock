package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.BlockingQueueSet;
import io.github.kusoroadeolu.sentinellock.entities.QueuedPendingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Thread.ofVirtual;

@Configuration
@RequiredArgsConstructor
public class MiscConfiguration {

    @Bean
    public Map<String, BlockingQueueSet<QueuedPendingRequest>> map(){
        return new ConcurrentHashMap<>();
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService(){
        return Executors.newScheduledThreadPool(100, ofVirtual().factory());
    }

    @Bean(name = "requestDispatcherExecutor")
    public ExecutorService requestDispatcherExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "pollingExecutor")
    public ExecutorService pollingExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }

}
