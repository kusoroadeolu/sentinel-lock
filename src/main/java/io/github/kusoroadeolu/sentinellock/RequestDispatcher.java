package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.entities.CompletableLease;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class RequestDispatcher {

    private final LeaseRegistry leaseRegistry;
    private final ExecutorService requestDispatcherExecutor;

    public RequestDispatcher(LeaseRegistry leaseRegistry, @Qualifier("requestDispatcherExecutor") ExecutorService requestDispatcherExecutor) {
        this.leaseRegistry = leaseRegistry;
        this.requestDispatcherExecutor = requestDispatcherExecutor;
    }

    public CompletableLease dispatchRequest(PendingRequest request){
        final var lease = new CompletableLease();
        this.requestDispatcherExecutor.submit(() -> this.leaseRegistry.ask(request, lease));
        return lease;
    }

}
