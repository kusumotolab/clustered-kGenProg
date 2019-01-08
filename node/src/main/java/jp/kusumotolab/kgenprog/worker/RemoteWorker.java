package jp.kusumotolab.kgenprog.worker;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class RemoteWorker implements Worker {

  final KGenProgClusterBlockingStub blockingStub;

  private RemoteWorker(final String name, final int port) {
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .build();
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
  }

  @Override
  public Single<GrpcRegisterProjectResponse> registerProject(
      final GrpcRegisterProjectRequest request, final int projectId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Single<GrpcExecuteTestResponse> executeTest(final GrpcExecuteTestRequest request) {
    return Single.create(emitter -> {
      final GrpcExecuteTestResponse response = blockingStub.executeTest(request);
      emitter.onSuccess(response);
    });
  }

  @Override
  public Single<GrpcUnregisterProjectResponse> unregisterProject(
      final GrpcUnregisterProjectRequest request) {
    return Single.create(emitter -> {
      final GrpcUnregisterProjectResponse response = blockingStub.unregisterProject(request);
      emitter.onSuccess(response);
    });
  }
}
