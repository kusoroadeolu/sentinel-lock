package io.github.kusoroadeolu.sentinellock.entities;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@RequiredArgsConstructor
public class Lease{

    @Setter
    long fencingToken;
    final ClientId leasedTo;
    final SyncKey key;
    final Instant leasedAt;
    final Instant expiresAt;
    final long leaseDurationInMs;

    public long fencingToken() {
        return fencingToken;
    }

    public long leaseDurationInMs() {
        return leaseDurationInMs;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant leasedAt() {
        return leasedAt;
    }

    public SyncKey key() {
        return key;
    }

    public ClientId leasedTo() {
        return leasedTo;
    }

    public boolean isExpired(){
        return Instant.now().isAfter(expiresAt);
    }

}
