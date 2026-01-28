package io.github.kusoroadeolu.sentinellock.entities;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.isNull;

public class BlockingQueueSet<E> {
    private final BlockingQueue<E> blockingQueue;
    private final Set<E> set;

    public BlockingQueueSet(int capacity) {
        this.blockingQueue = new LinkedBlockingQueue<>(capacity);
        this.set = ConcurrentHashMap.newKeySet();
    }

    public boolean offer(E e){
        if (this.set.add(e)){
            final var canPut = this.blockingQueue.offer(e);
            if (!canPut) {
                this.set.remove(e);
                return false;
            }
            return true;
        }

        return false;
    }

    public Optional<E> poll(){
        final var e = this.blockingQueue.poll();
        if (!isNull(e)) this.set.remove(e);
        return Optional.ofNullable(e);
    }

    public boolean remove(E e){
        final var removed = this.blockingQueue.remove(e);
        this.set.remove(e);
        return false;
    }


}
