package io.github.kusoroadeolu.sentinellock.entities;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public final class Synchronizer {
    private final ClientId id;
    private final AtomicLong leaseCount; //Num of times this synchronizer has been leased

    public Synchronizer(ClientId id){
        this(id, new AtomicLong(0));
    }

    public Lease newLease(long fencingToken, long leaseDuration){
        Instant now = Instant.now();
        return new Lease(fencingToken, this.id,  now, now.plusMillis(leaseDuration), leaseDuration);
    }

    public void increment(){
        this.leaseCount.incrementAndGet();
    }

    public long getLeaseCount(){
        return this.leaseCount.get();
    }
}
