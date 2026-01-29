package io.github.kusoroadeolu.sentinellock.entities;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class CompletableLease {
    private final CompletableFuture<Lease> future;
    @Setter
    @Getter
    private volatile Status status;
    private volatile Lease lease;

    public CompletableLease() {
        this.future = new CompletableFuture<>();
        this.status = Status.WAITING;
    }

    public void complete(@NonNull Lease lease){
        if (this.status != Status.WAITING) return;
        this.future.complete(lease);
        this.setStatus(Status.ACQUIRED);
        this.lease = lease;
    }

    public void completeExceptionally(@NonNull Lease lease, @NonNull Status status){
        if (this.status != Status.WAITING) return;
        this.future.complete(lease);
        this.setStatus(status);
        this.lease = lease;
    }

    public Lease join(){
        this.future.join();
        return this.lease;
    }

    public CompletableLease whenComplete(BiConsumer<? super Lease, ? super Throwable> action){
        this.future.whenComplete(action);
        return this;
    }


    /*
    * Acquired -> Lease has been acquired
    * Waiting -> We are still waiting for the lease
    * Timed out -> Timed out waiting for the lease
    * */
    public enum Status{
        ACQUIRED, WAITING, TIMED_OUT, FAILED
    }

}
