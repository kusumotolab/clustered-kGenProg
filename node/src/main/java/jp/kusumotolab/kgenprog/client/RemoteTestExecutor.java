package jp.kusumotolab.kgenprog.client;

import java.nio.file.Path;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.Configuration;
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
  private final Configuration config;

  public RemoteTestExecutor(final Configuration config, final String name, final int port) {
    this.config = config;
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .build();
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
  }

  public RemoteTestExecutor(final Configuration config, final ManagedChannel managedChannel) {
    this.config = config;
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
  }

  @Override
  public TestResults exec(final Variant variant) {
    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setGene(Serializer.serialize(variant.getGene()))
        .build();
    final GrpcExecuteTestResponse response = blockingStub.executeTest(request);
    final Path rootPath = config.getTargetProject().rootPath;
    return Serializer.deserialize(rootPath, response.getTestResults());
  }
}
