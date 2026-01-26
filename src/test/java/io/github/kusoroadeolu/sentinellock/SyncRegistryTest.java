package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.entities.ClientId;
import io.github.kusoroadeolu.sentinellock.entities.LeaseResponse;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;
import io.github.kusoroadeolu.sentinellock.entities.SyncKey;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

//Test the normal flow, acquire -> success
//Test when a sync is already leased, then released, result should be I get a CompletedLeaseResponse
//Test when a sync is already leased, but we exceed the max wait time, should get a waiting lease response
// Test when obtaining a sync failed, should get a failed lease response
@SpringBootTest
class SyncRegistryTest {

    @Autowired
    private SyncRegistry syncRegistry;

    @Test
    public void shouldReturnACompletedLeaseResponse_onUnAcquiredLease() throws ExecutionException, InterruptedException, TimeoutException {
        ClientId id = new ClientId("clId");
        SyncKey syncKey = new SyncKey("ck");

        PendingRequest request = new PendingRequest(id, syncKey, 2000, 4000);
        LeaseResponse response = syncRegistry.ask(request);
        assertInstanceOf(LeaseResponse.CompletedLeaseResponse.class, response);
    }

    @Test
    public void shouldReturnWaitingLeaseResponse_whenResourceAlreadyLeased()
            throws ExecutionException, InterruptedException {
        SyncKey syncKey = new SyncKey("resource-2");
        ClientId client1 = new ClientId("client-1");
        ClientId client2 = new ClientId("client-2");

        PendingRequest request1 = new PendingRequest(client1, syncKey, 3000, 5000);
        LeaseResponse response1 = syncRegistry.ask(request1);
        assertInstanceOf(LeaseResponse.CompletedLeaseResponse.class, response1);

        PendingRequest request2 = new PendingRequest(client2, syncKey, 500, 1000);
        LeaseResponse response2 = syncRegistry.ask(request2);

        assertInstanceOf(LeaseResponse.WaitingLeaseResponse.class, response2);
    }

    @Test
    public void shouldEventuallyAcquireLease_whenFirstLeaseExpires()
            throws ExecutionException, InterruptedException {
        SyncKey syncKey = new SyncKey("resource-3");
        ClientId client1 = new ClientId("client-1");
        ClientId client2 = new ClientId("client-2");

        // Client 1 acquires with short lease
        PendingRequest request1 = new PendingRequest(client1, syncKey, 500, 1000);
        syncRegistry.ask(request1);

        // Wait for lease to expire
        Thread.sleep(600);

        PendingRequest request2 = new PendingRequest(client2, syncKey, 2000, 4000);
        LeaseResponse response2 = syncRegistry.ask(request2);

        assertInstanceOf(LeaseResponse.CompletedLeaseResponse.class, response2);
    }

    @Test
    public void shouldIncrementFencingToken_onSuccessiveLeases()
            throws ExecutionException, InterruptedException {
        SyncKey syncKey = new SyncKey("resource-4");

        PendingRequest request1 = new PendingRequest(
                new ClientId("client-1"), syncKey, 100, 500
        );
        LeaseResponse.CompletedLeaseResponse response1 =
                (LeaseResponse.CompletedLeaseResponse) syncRegistry.ask(request1);

        Thread.sleep(150);

        PendingRequest request2 = new PendingRequest(
                new ClientId("client-2"), syncKey, 100, 500
        );
        LeaseResponse.CompletedLeaseResponse response2 =
                (LeaseResponse.CompletedLeaseResponse) syncRegistry.ask(request2);

        assertTrue(response2.fencingToken() > response1.fencingToken());
    }

}