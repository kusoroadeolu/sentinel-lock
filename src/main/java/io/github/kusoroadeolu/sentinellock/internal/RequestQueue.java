package io.github.kusoroadeolu.sentinellock.internal;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.*;

import io.github.kusoroadeolu.sentinellock.entities.Lease.TimedOutLease;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static io.github.kusoroadeolu.sentinellock.entities.CompletableLease.Status.*;
import static io.github.kusoroadeolu.sentinellock.entities.Lease.FailedLease.Cause.QUEUE_FULL;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestQueue {
    private final Map<String, BlockingQueueSet<QueuedPendingRequest>> map;
    private final SentinelLockConfigProperties sentinelLockConfigProperties;
    private final ScheduledExecutorService scheduledExecutorService;


    public boolean offer(@NonNull PendingRequest request, CompletableLease future){
       final var key = request.syncKey().key();
       final var queue =
               this.map.computeIfAbsent(key , (_) -> new BlockingQueueSet<>(this.sentinelLockConfigProperties.maxQueuedRegistryClients()));
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
        return this.map.get(syncKey.key()).poll();
    }

    public boolean remove(@NonNull QueuedPendingRequest qpr){
        final var key = qpr.request().syncKey().key();
        return this.map.get(key).remove(qpr);
    }

    public void handleFutureIfQueueFull(boolean notFull, CompletableLease future, ClientId clientId, SyncKey syncKey){
        if (!notFull) {
            future.completeExceptionally(new Lease.FailedLease(QUEUE_FULL), FAILED);
            log.info("Failed to add client: {} requesting for sync key: {} to queue", clientId, syncKey);
        }else {
            log.info("Added client: {} requesting for sync key: {} to queue", clientId, syncKey);
        }
    }

}
