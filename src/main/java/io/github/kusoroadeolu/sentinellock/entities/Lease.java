package io.github.kusoroadeolu.sentinellock.entities;

import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record Lease(
        long fencingToken,
        @NonNull ClientId leasedTo,
        @NonNull Instant leasedAt,
        @NonNull Instant expiresAt,
        long leaseDurationInMs
) {

    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

}
