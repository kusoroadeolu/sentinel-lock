package io.github.kusoroadeolu.sentinellock.entities;

import io.github.kusoroadeolu.sentinellock.annotations.Proto;

@Proto
public interface Lease {
    io.github.kusoroadeolu.Lease toProto();

    record CompleteLease(SyncKey key, long fencingToken) implements Lease{
        public io.github.kusoroadeolu.Lease toProto(){
            final var complete = io.github.kusoroadeolu.CompleteLease
                    .newBuilder()
                    .setFencingToken(this.fencingToken)
                    .setKey(this.key.toProto())
                    .build();

            return io.github.kusoroadeolu.Lease.newBuilder()
                    .setComplete(complete)
                    .build();
        }
    }

    record FailedLease(Cause cause) implements Lease{
        @Override
        public io.github.kusoroadeolu.Lease toProto() {
            final var failed = io.github.kusoroadeolu.FailedLease
                    .newBuilder()
                    .setCause(cause.toProto())
                    .build();

            return io.github.kusoroadeolu.Lease.newBuilder()
                    .setFailed(failed)
                    .build();
        }

        @Proto
        public enum Cause{
            ERR, QUEUE_FULL, INVALID_LEASE_DURATION;

            io.github.kusoroadeolu.FailedLease.Cause toProto(){
                return switch (this){
                    case ERR -> io.github.kusoroadeolu.FailedLease.Cause.ERR;
                    case QUEUE_FULL -> io.github.kusoroadeolu.FailedLease.Cause.QUEUE_FULL;
                    case INVALID_LEASE_DURATION -> io.github.kusoroadeolu.FailedLease.Cause.INVALID_LEASE_DURATION;
                };
            }
        }

    }
    record TimedOutLease() implements Lease{
        @Override
        public io.github.kusoroadeolu.Lease toProto() {
            final var timedOut = io.github.kusoroadeolu.TimedOutLease
                    .getDefaultInstance();

            return io.github.kusoroadeolu.Lease.newBuilder()
                    .setTimedOut(timedOut)
                    .build();
        }
    }

    default CompleteLease asCompleteLease(){
        return (CompleteLease) this;
    }
}
