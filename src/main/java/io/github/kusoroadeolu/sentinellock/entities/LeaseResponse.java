package io.github.kusoroadeolu.sentinellock.entities;

public record LeaseResponse(long fencingToken, ClientId id) {

}
