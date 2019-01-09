package jp.kusumotolab.kgenprog.coordinator;


import io.grpc.stub.StreamObserver;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterImplBase;

public class KGenProgCluster extends KGenProgClusterImplBase {

  private final Coordinator coordinator;

  public KGenProgCluster(final Coordinator coordinator) {
    this.coordinator = coordinator;
  }

  @Override
  public void registerProject(final GrpcRegisterProjectRequest request,
      final StreamObserver<GrpcRegisterProjectResponse> responseObserver) {
    coordinator.registerProject(request, responseObserver);
  }

  @Override
  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    coordinator.executeTest(request, responseObserver);
  }

  @Override
  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    coordinator.unregisterProject(request, responseObserver);
  }
}
