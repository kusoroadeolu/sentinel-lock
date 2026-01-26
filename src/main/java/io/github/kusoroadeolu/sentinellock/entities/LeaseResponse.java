package io.github.kusoroadeolu.sentinellock.entities;

import java.util.concurrent.CompletableFuture;

public interface LeaseResponse {
    record CompletedLeaseResponse(SyncKey key, long fencingToken) implements LeaseResponse{}

    record WaitingLeaseResponse(CompletableFuture<CompletedLeaseResponse> future) implements LeaseResponse{}
    

    enum FailedLeaseResponse implements LeaseResponse{
        FAILED
    }

}