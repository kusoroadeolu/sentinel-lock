package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

//Test the normal flow, acquire -> success
//Test when a sync is already leased, then released, result should be I get a CompletedLeaseResponse
//Test when a sync is already leased, but we exceed the max wait time, should get a waiting lease response
// Test when obtaining a sync failed, should get a failed lease response
@SpringBootTest
@Slf4j
class SyncRegistryTest {

    @Autowired
    private SyncRegistry syncRegistry;

    @Test
    public void shouldReturnACompletedLeaseResponse_onUnAcquiredLease() {
        ClientId id = new ClientId("client-1");
        SyncKey syncKey = new SyncKey("resource-1");

        PendingRequest request = new PendingRequest(id, syncKey, 2000, 4000);
        CompletableLease future = new CompletableLease();
        syncRegistry.ask(request, future);
        assertEquals(CompletableLease.Status.ACQUIRED, future.getStatus());
    }

    @Test
    public void shouldReturnWaitingLeaseResponse_whenResourceAlreadyLeased() {
        SyncKey syncKey = new SyncKey("resource-2");
        ClientId client1 = new ClientId("client-1");
        ClientId client2 = new ClientId("client-2");
        CompletableLease future = new CompletableLease();
        PendingRequest request1 = new PendingRequest(client1, syncKey, 3000, 5000);
        syncRegistry.ask(request1, future);
        assertEquals(CompletableLease.Status.ACQUIRED, future.getStatus());

        CompletableLease future1 = new CompletableLease();
        PendingRequest request2 = new PendingRequest(client2, syncKey, 500, 1000);
        syncRegistry.ask(request2, future1);
        assertEquals(CompletableLease.Status.WAITING, future1.getStatus());
    }

    @Test
    public void shouldEventuallyAcquireLease_whenFirstLeaseExpires()
            throws ExecutionException, InterruptedException {
        SyncKey syncKey = new SyncKey("resource-3");
        ClientId client1 = new ClientId("client-1");
        ClientId client2 = new ClientId("client-2");

        // Client 1 acquires with short lease
        CompletableLease future1 = new CompletableLease();
        PendingRequest request1 = new PendingRequest(client1, syncKey, 500, 1000);
        syncRegistry.ask(request1, future1);

        // Wait for lease to expire
        Thread.sleep(600);

        CompletableLease future2 = new CompletableLease();
        PendingRequest request2 = new PendingRequest(client2, syncKey, 2000, 4000);
        syncRegistry.ask(request2, future2);

        assertEquals(CompletableLease.Status.ACQUIRED, future2.getStatus());
    }

    @Test
    public void shouldIncrementFencingToken_onSuccessiveLeases()
            throws  InterruptedException {
        SyncKey syncKey = new SyncKey("resource-4");

        PendingRequest request1 = new PendingRequest(
                new ClientId("client-1"), syncKey, 100, 500
        );

        CompletableLease future1 = new CompletableLease();
        syncRegistry.ask(request1, future1);

        Thread.sleep(150);

        CompletableLease future2 = new CompletableLease();
        PendingRequest request2 = new PendingRequest(
                new ClientId("client-2"), syncKey, 100, 500
        );
        syncRegistry.ask(request2, future2);
        Lease.CompleteLease l1 = future1.join().asCompleteLease();
        Lease.CompleteLease l2 =  future2.join().asCompleteLease();
        assertTrue(l2.fencingToken() > l1.fencingToken());
    }



}