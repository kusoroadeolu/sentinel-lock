package io.github.kusoroadeolu.sentinellock.entities;


import io.github.kusoroadeolu.sentinellock.entities.LeaseResponse.CompletedLeaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

//A pending request that has already been placed in the client map
@RequiredArgsConstructor
public class QueuedPendingRequest {
    final PendingRequest request;
    final CompletableFuture<CompletedLeaseResponse> future;
    @Setter
    ScheduledFuture<?> scheduled;

    public PendingRequest request() {
        return this.request;
    }

    public ScheduledFuture<?> scheduled() {
        return this.scheduled;
    }

    public CompletableFuture<CompletedLeaseResponse> future() {
        return future;
    }

    public void cancelAndCompleteFuture(){
        this.scheduled.cancel(true);
    }
}
