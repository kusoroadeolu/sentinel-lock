package io.github.kusoroadeolu.sentinellock.entities;

//A dummy class that we basically use to manage the idle lifecycle of lock state
public record Synchronizer(Long leaseCount) {

    public Synchronizer{
        if (leaseCount == null) leaseCount = 0L;
    }
}
