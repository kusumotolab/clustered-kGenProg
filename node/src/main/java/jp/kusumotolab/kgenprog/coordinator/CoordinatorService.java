package jp.kusumotolab.kgenprog.coordinator;

import io.grpc.stub.StreamObserver;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc.CoordinatorServiceImplBase;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;

public class CoordinatorService extends CoordinatorServiceImplBase {

  private final Coordinator coordinator;

  public CoordinatorService(final Coordinator coordinator) {
    this.coordinator = coordinator;
  }

  @Override
  public void registerWorker(final GrpcRegisterWorkerRequest request,
      final StreamObserver<GrpcRegisterWorkerResponse> responseObserver) {
    coordinator.registerWorker(request, responseObserver);
  }

  @Override
  public void getProject(final GrpcGetProjectRequest request,
      final StreamObserver<GrpcGetProjectResponse> responseObserver) {
    coordinator.getProject(request, responseObserver);
  }
}
