package io.github.kusoroadeolu.sentinellock.config;

import io.github.kusoroadeolu.sentinellock.entities.QueuedPendingRequest;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Thread.ofVirtual;

@Configuration
@RequiredArgsConstructor
public class MiscConfig {

    @Bean
    public Map<String, BlockingQueue<QueuedPendingRequest>> map(){
        return new ConcurrentHashMap<>();
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService(){
        return Executors.newScheduledThreadPool(50, ofVirtual().factory());
    }

    public Executor leaseThreadPool(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
