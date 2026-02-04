package io.github.kusoroadeolu.sentinellock.entities;

import io.github.kusoroadeolu.sentinellock.internal.LeaseRegistry;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class CompletableLeaseReleaseResult {
    private final CompletableFuture<LeaseReleaseResult> future;

    public CompletableLeaseReleaseResult() {
        this.future = new CompletableFuture<>();
    }

    public void complete(LeaseReleaseResult result) {
        future.complete(result);
    }

    public void whenComplete(BiConsumer<? super LeaseReleaseResult, ? super Throwable> action){
        future.whenComplete(action);
    }
}
