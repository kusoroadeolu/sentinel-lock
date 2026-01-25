package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.QueuedPendingRequest;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class RequestQueue {
    private final Map<String, BlockingQueue<QueuedPendingRequest>> map;
    private final SentinelLockConfigProperties sentinelLockConfigProperties;
    private final ScheduledExecutorService scheduledExecutorService;


    public boolean offer(@NonNull PendingRequest request, CompletableFuture<?> future){
       final var key = request.syncKey().key();
       final var queue =
               this.map.computeIfAbsent(key , (_) -> new LinkedBlockingQueue<>(sentinelLockConfigProperties.maxQueuedRegistryClients()));
       final var qpr = new QueuedPendingRequest(request, future);
       final var qd = qpr.request().queueDuration();
       var scheduled = this.scheduledExecutorService.schedule(() -> this.remove(qpr), qd, TimeUnit.MILLISECONDS);
       qpr.setScheduled(scheduled);
       return queue.offer(qpr);
    }

    public Optional<QueuedPendingRequest> poll(@NonNull SyncKey syncKey){
        return Optional.ofNullable(this.map.get(syncKey.key()).poll());
    }

    public boolean remove(@NonNull QueuedPendingRequest qpr){
        final var key = qpr.request().syncKey().key();
        return this.map.get(key).remove(qpr);
    }

}
