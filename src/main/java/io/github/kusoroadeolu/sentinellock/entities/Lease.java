package io.github.kusoroadeolu.sentinellock.entities;

public interface Lease {
    record CompleteLease(SyncKey key, long fencingToken) implements Lease{
    }

    record FailedLease(Cause cause) implements Lease{
        public enum Cause{
            ERR, QUEUE_FULL
        }

    }
    record TimedOutLease() implements Lease{}

    default CompleteLease asCompleteLease(){
        return (CompleteLease) this;
    }
}
