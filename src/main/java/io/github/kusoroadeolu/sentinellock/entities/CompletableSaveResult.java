package io.github.kusoroadeolu.sentinellock.entities;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class CompletableSaveResult {
    private final CompletableFuture<SaveResult> future;

    public CompletableSaveResult() {
        this.future = new CompletableFuture<>();
    }

    public void complete(SaveResult result) {
        future.complete(result);
    }

    public void whenComplete(BiConsumer<? super SaveResult, ? super Throwable> action){
        future.whenComplete(action);
    }

    public CompletableFuture<SaveResult> future() {
        return future;
    }
}
