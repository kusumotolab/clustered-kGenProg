package jp.kusumotolab.kgenprog.coordinator;

import java.util.concurrent.TimeUnit;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class RemoteWorker implements Worker {

  private final int workerId;
  private final String workerName;
  private final KGenProgClusterBlockingStub blockingStub;
  private final ManagedChannel managedChannel;

  public RemoteWorker(final int workerId, final String name, final int port) {
    this.workerId = workerId;
    this.workerName = name + port;
    managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .keepAliveTime(ClusterConfiguration.DEFAULT_KEEPALIVE_SECONDS, TimeUnit.SECONDS)
        .maxInboundMessageSize(Integer.MAX_VALUE)
        .build();
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
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

  @Override
  public void finish() {
    managedChannel.shutdown();
  }

  @Override
  public int getId() {
    return workerId;
  }

  @Override
  public String getName() {
    return workerName;
  }
}
