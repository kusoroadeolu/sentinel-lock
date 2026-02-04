package io.github.kusoroadeolu.sentinellock.internal;

import io.github.kusoroadeolu.sentinellock.entities.CompletableLease;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestDispatcher {
    private final LeaseRegistry leaseRegistry;
    private final ExecutorService requestDispatcherExecutor;

    public CompletableLease dispatchRequest(PendingRequest request){
        final var lease = new CompletableLease();
        this.requestDispatcherExecutor.submit(() -> this.leaseRegistry.ask(request, lease));
        return lease;
    }

    public CompletableFuture<>

}
