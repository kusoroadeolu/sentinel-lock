package io.github.kusoroadeolu.sentinellock.entities;

import java.util.concurrent.CompletableFuture;

public interface LeaseResponse {
    public record CompletedLeaseResponse(SyncKey key, long fencingToken) implements LeaseResponse{
    }

    public record WaitingLeaseResponse(CompletableFuture<?> future) implements LeaseResponse{

    }

    enum FailedLeaseResponse implements LeaseResponse{
        FAILED
    }

}