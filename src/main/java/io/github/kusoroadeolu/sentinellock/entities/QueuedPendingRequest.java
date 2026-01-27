package io.github.kusoroadeolu.sentinellock.entities;


import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ScheduledFuture;

//A pending request that has already been placed in the client map
@RequiredArgsConstructor
public class QueuedPendingRequest {
    final PendingRequest request;
    final CompletableLease future;
    @Setter
    ScheduledFuture<?> scheduled;

    public PendingRequest request() {
        return this.request;
    }

    public ScheduledFuture<?> scheduled() {
        return this.scheduled;
    }

    public CompletableLease future() {
        return future;
    }

    public void cancelAndCompleteFuture(){
        this.scheduled.cancel(true);
    }
}
