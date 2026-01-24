package io.github.kusoroadeolu.sentinellock.entities;

import lombok.NonNull;

import java.time.Instant;

//Lease duration is in millis
public record PendingRequest(@NonNull ClientId id, long requestedLeaseDuration, @NonNull Instant queuedAt) {
    public PendingRequest{
    }
}
