package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.configprops.SentinelLockConfigProperties;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
public class ClientMap {
    private final Map<SyncKey, BlockingQueue<PendingRequest>> map;
    private final SentinelLockConfigProperties sentinelLockConfigProperties;

    public boolean offer(@NonNull PendingRequest request){
       var queue =
               this.map.computeIfAbsent(request.syncKey(), (_) -> new LinkedBlockingQueue<>(sentinelLockConfigProperties.maxQueuedRegistryClients()));
       return queue.offer(request);
    }

    public PendingRequest poll(@NonNull SyncKey id){
        return this.map.get(id).poll();
    }

    public boolean remove(@NonNull PendingRequest request){
        return this.map.get(request.syncKey()).remove(request);
    }

}
