package io.github.kusoroadeolu.sentinellock.entities;

import io.github.kusoroadeolu.SyncKeyOrBuilder;
import io.github.kusoroadeolu.sentinellock.annotations.Proto;
import org.jspecify.annotations.NonNull;

@Proto
public record SyncKey(
       @NonNull String key
) {
     io.github.kusoroadeolu.SyncKey toProto(){
        return io.github.kusoroadeolu.SyncKey
                .newBuilder()
                .setSyncKey(this.key)
                .build();
    }

    public static SyncKey fromProto(io.github.kusoroadeolu.SyncKey key){
         return new SyncKey(key.getSyncKey());
    }
}
