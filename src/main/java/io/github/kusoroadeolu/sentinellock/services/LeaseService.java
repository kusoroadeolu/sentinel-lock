package io.github.kusoroadeolu.sentinellock.services;


import io.github.kusoroadeolu.Lease;
import io.github.kusoroadeolu.LeaseRequest;
import io.github.kusoroadeolu.LeaseServiceGrpc;
import io.github.kusoroadeolu.sentinellock.RequestDispatcher;
import io.github.kusoroadeolu.sentinellock.entities.PendingRequest;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import static io.github.kusoroadeolu.sentinellock.entities.ClientId.fromProto;
import static io.github.kusoroadeolu.sentinellock.entities.SyncKey.fromProto;

@GrpcService
@RequiredArgsConstructor
public class LeaseService extends LeaseServiceGrpc.LeaseServiceImplBase {

    private final RequestDispatcher dispatcher;

    public void acquireLease(LeaseRequest request, StreamObserver<Lease> responseObserver) {
        final var completable = this.dispatcher.dispatchRequest(this.fromLeaseReq(request));
        completable.whenComplete((lease, err) -> {
            if (err != null) responseObserver.onError(err);
            else {
                responseObserver.onNext(lease.toProto());
                responseObserver.onCompleted();
            }
        });
        
    }


     PendingRequest fromLeaseReq(LeaseRequest request){
        final var syncKey = fromProto(request.getKey());
        final var clientId = fromProto(request.getId());
        return new PendingRequest(clientId, syncKey, request.getRequestedLeaseDuration(), request.getQueueDuration());
    }
}
