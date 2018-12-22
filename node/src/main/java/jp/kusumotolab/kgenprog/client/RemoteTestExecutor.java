package jp.kusumotolab.kgenprog.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.Serializer;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class RemoteTestExecutor implements TestExecutor {

  private final KGenProgClusterBlockingStub blockingStub;

  public RemoteTestExecutor(final String name, final int port) {
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .build();
    blockingStub = KGenProgClusterGrpc.newBlockingStub(
        managedChannel);
  }

  @Override
  public TestResults exec(final Variant variant) {
    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setGene(Serializer.serialize(variant.getGene()))
        .build();
    final GrpcExecuteTestResponse response = blockingStub.executeTest(request);
    return Serializer.deserialize(response.getTestResults());
  }
}
