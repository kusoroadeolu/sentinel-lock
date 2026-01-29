package io.github.kusoroadeolu.sentinellock.entities;

import lombok.NonNull;

public record LeaseRequest(@NonNull ClientId id, @NonNull SyncKey syncKey , long requestedLeaseDuration, long queueDuration) {
    PendingRequest toPendingRequest(){
        return new PendingRequest(id, syncKey, requestedLeaseDuration, queueDuration);
    }
}
