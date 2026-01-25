package io.github.kusoroadeolu.sentinellock.entities;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public final class Synchronizer {
    private final SyncKey key;
    private final AtomicLong leaseCount; //Num of times this synchronizer has been leased
    private final Lock leaseLock;
    private Lease currentLease;

    public Synchronizer(SyncKey id){
        this(id, new AtomicLong(0), new ReentrantLock(true));
    }

    public LeaseResult newLease(long leaseDuration, ClientId id){
        var result = LeaseResult.AlreadyLeased.NONE;
        this.leaseLock.lock();
        try {
            if (this.currentLease != null && !this.currentLease.isExpired()) return result;
            var now = Instant.now();
            this.currentLease = new Lease(id, this.key ,now, now.plusMillis(leaseDuration), leaseDuration);
        }finally {
            this.leaseLock.unlock();
        }

        this.incrementLeaseCount();
        return new LeaseResult.Success(this.currentLease);
    }

    void incrementLeaseCount(){
        this.leaseCount.incrementAndGet();
    }

    public long getLeaseCount(){
        return this.leaseCount.get();
    }


    public sealed interface LeaseResult permits LeaseResult.AlreadyLeased, LeaseResult.Success {
        enum AlreadyLeased implements LeaseResult{  //represents if this sync is currently leased
            NONE
        }

        record Success(Lease lease) implements LeaseResult{ //represents if this sync is unleased

        }
    }
}
