package io.github.kusoroadeolu.sentinellock.entities;


import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.ScheduledFuture;

//A pending request that has already been placed in the client map
@RequiredArgsConstructor
public class QueuedPendingRequest {
    final PendingRequest request;
    @Setter
    ScheduledFuture<?> scheduled;

    public PendingRequest request() {
        return this.request;
    }

    public ScheduledFuture<?> scheduled() {
        return this.scheduled;
    }

    public void cancelFuture(){
        this.scheduled.cancel(true);
    }
}
