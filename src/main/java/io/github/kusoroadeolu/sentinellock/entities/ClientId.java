package io.github.kusoroadeolu.sentinellock.entities;

import io.github.kusoroadeolu.sentinellock.annotations.Proto;

@Proto
public record ClientId(
        String id
) {

    io.github.kusoroadeolu.ClientId toProto(){
        return io.github.kusoroadeolu.ClientId.newBuilder().setClientId(this.id).build();
    }

    public static ClientId fromProto(io.github.kusoroadeolu.ClientId id){
        return new ClientId(id.getClientId());
    }
}
