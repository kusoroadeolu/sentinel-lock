package io.github.kusoroadeolu.sentinellock.grpc;

import io.github.kusoroadeolu.ClientId;
import io.github.kusoroadeolu.LeaseRequest;
import io.github.kusoroadeolu.LeaseServiceGrpc;
import io.github.kusoroadeolu.SyncKey;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LeaseServiceTest {
    @Test
    void testGrpcService() {
        var channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        var stub = LeaseServiceGrpc.newBlockingStub(channel);

        var request = LeaseRequest
                .newBuilder()
                .setKey(SyncKey.newBuilder().setSyncKey("test-resource"))
                .setId(ClientId.newBuilder().setClientId("test-client"))
                .setRequestedLeaseDuration(5000)
                .setQueueDuration(10000)
                .build();

        var iterator = stub.acquireLease(request);
        assertTrue(iterator.hasNext());
        var lease = iterator.next();
        assertNotNull(lease);
        assertNotNull(lease.getComplete());
        var complete = lease.getComplete();
        assertEquals(1, complete.getFencingToken());

        channel.shutdown();
    }
}