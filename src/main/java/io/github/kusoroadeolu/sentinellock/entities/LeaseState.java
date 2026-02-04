package io.github.kusoroadeolu.sentinellock.entities;

import java.time.Instant;

public record LeaseState(
        SyncKey syncKey,
        ClientId currentHolder,
        Long fencingToken,
        Instant acquiredAt,
        Instant expiresAt,
        Long leaseDuration
) {

    public LeaseState{
        if (fencingToken == null) fencingToken = 0L;
        if (leaseDuration == null) leaseDuration = 0L;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}