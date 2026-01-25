package io.github.kusoroadeolu.sentinellock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SyncRegistryTest {
    @Test
    public void testCompletableFutureSemantics(){
        var future = new CompletableFuture<>();
        assertFalse(future.isDone());
        future.complete(null);
        assertTrue(future.isDone());
    }
}