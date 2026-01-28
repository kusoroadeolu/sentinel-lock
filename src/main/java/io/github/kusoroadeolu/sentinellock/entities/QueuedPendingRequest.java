package io.github.kusoroadeolu.sentinellock.entities;


import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Objects;
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

    public CompletableLease future() {
        return future;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        QueuedPendingRequest that = (QueuedPendingRequest) object;
        return Objects.equals(request.syncKey(), that.request.syncKey()) && Objects.equals(request.id(), that.request.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(request.syncKey(), request.id());
    }
}
