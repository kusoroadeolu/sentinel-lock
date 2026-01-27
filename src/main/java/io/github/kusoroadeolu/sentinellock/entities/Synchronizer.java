package io.github.kusoroadeolu.sentinellock.entities;

//A dummy class that we basically use to manage the idle lifecycle of lock state
public record Synchronizer(Long currentFencingToken) {

    public Synchronizer{
        if (currentFencingToken == null) currentFencingToken = 0L;
    }
}
