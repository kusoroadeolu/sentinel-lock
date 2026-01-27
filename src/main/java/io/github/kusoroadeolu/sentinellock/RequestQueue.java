package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;

import io.github.kusoroadeolu.sentinellock.entities.Lease.TimedOutLease;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static io.github.kusoroadeolu.sentinellock.entities.CompletableLease.Status.*;

@Service
@RequiredArgsConstructor
public class RequestQueue {
    private final Map<String, BlockingQueue<QueuedPendingRequest>> map;
    private final SentinelLockConfigProperties sentinelLockConfigProperties;
    private final ScheduledExecutorService scheduledExecutorService;


    public boolean offer(@NonNull PendingRequest request, CompletableLease future){
       final var key = request.syncKey().key();
       final var queue =
               this.map.computeIfAbsent(key , (_) -> new LinkedBlockingQueue<>(this.sentinelLockConfigProperties.maxQueuedRegistryClients()));
       final var qpr = new QueuedPendingRequest(request, future);
       final var qd = qpr.request().queueDuration();
       final var scheduled = this.scheduledExecutorService.schedule(() -> {
           this.remove(qpr);
           future.completeExceptionally(new TimedOutLease(), TIMED_OUT); //Future timed out waiting
       }, qd, TimeUnit.MILLISECONDS);
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
