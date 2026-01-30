package io.github.kusoroadeolu.sentinellock;

import io.github.kusoroadeolu.sentinellock.FencingTokenValidator.SaveResult;
import io.github.kusoroadeolu.sentinellock.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FencingTokenCheckerTest {
    @Autowired
    private LeaseRegistry leaseRegistry;

    @Autowired
    private RedisTemplate<Object, Object> clientTemplate;

    @Autowired
    private FencingTokenValidator fencingTokenValidator;



    @Test
    public void onCurrentFencingToken_shouldReturnSuccessResult(){
        ClientId client1 = new ClientId("client-1");
        SyncKey syncKey = new SyncKey("resource-a");
        CompletableLease future = new CompletableLease();
        PendingRequest request1 = new PendingRequest(client1, syncKey, 3000, 5000);
        leaseRegistry.ask(request1, future);
        assertEquals(CompletableLease.Status.ACQUIRED, future.getStatus());
        Lease lease = future.join();
        SaveResult result = fencingTokenValidator.validateAndSave(lease, "changed_value");
        assertInstanceOf(SaveResult.Success.class, result);
        assertNotNull(clientTemplate.opsForValue().get(syncKey.key()));
    }

    @Test
    public void onStaleFencingToken_shouldReturnFailedResult(){
        SyncKey syncKey = new SyncKey("resource-b");
        Lease lease1 = new Lease.CompleteLease(syncKey, new ClientId(""),-1);
        SaveResult result = fencingTokenValidator.validateAndSave(lease1,"changed_value");
        assertInstanceOf(SaveResult.Failed.class, result);
    }

    @Test
    public void onModificationDuringTransaction_shouldReturnFailedResult() throws InterruptedException {
        SyncKey syncKey = new SyncKey("resource-b");

        ClientId client1 = new ClientId("client-1");
        CompletableLease future1 = new CompletableLease();
        PendingRequest request1 = new PendingRequest(client1, syncKey, 100, 5000);
        leaseRegistry.ask(request1, future1);
        assertEquals(CompletableLease.Status.ACQUIRED, future1.getStatus());
        Thread.sleep(200);

        ClientId client2 = new ClientId("client-2");
        CompletableLease future2 = new CompletableLease();
        PendingRequest request2 = new PendingRequest(client2, syncKey, 500, 5000);
        leaseRegistry.ask(request2, future2);
        assertEquals(CompletableLease.Status.ACQUIRED, future2.getStatus());


        Lease.CompleteLease lease1 = future1.join().asCompleteLease();
        Lease.CompleteLease lease2 = future2.join().asCompleteLease();

        assertTrue(lease1.fencingToken() < lease2.fencingToken());


        SaveResult result2 = fencingTokenValidator.validateAndSave(lease2, "changed_value2");
        assertInstanceOf(SaveResult.Success.class, result2);

        SaveResult result1 = fencingTokenValidator.validateAndSave(lease2, "changed_value1");
        assertInstanceOf(SaveResult.Failed.class, result1);
    }
}