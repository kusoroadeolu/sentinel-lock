package io.github.kusoroadeolu.sentinellock.entities;

import lombok.Builder;
import lombok.NonNull;

import java.time.Instant;

//Lease duration is in millis
public record PendingRequest(@NonNull ClientId id, @NonNull SyncKey syncKey ,long requestedLeaseDuration, long queueDuration) {
}
