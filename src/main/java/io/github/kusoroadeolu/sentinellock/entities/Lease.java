package io.github.kusoroadeolu.sentinellock.entities;

public interface Lease {
    record CompleteLease(SyncKey key, long fencingToken) implements Lease{
    }

    record FailedLease(Cause cause) implements Lease{
        public enum Cause{
            ERR, QUEUE_FULL, INVALID_LEASE_DURATION
        }

    }
    record TimedOutLease() implements Lease{}

    default CompleteLease asCompleteLease(){
        return (CompleteLease) this;
    }
}
