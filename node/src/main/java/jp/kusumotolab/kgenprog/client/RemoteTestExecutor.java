package jp.kusumotolab.kgenprog.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.grpc.Coordinator;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.ProjectZipper;
import jp.kusumotolab.kgenprog.grpc.Serializer;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class RemoteTestExecutor implements TestExecutor {

  private final KGenProgClusterBlockingStub blockingStub;
  private final Configuration config;
  private Optional<Integer> projectId = Optional.empty();

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
    if (!projectId.isPresent()) {
      return EmptyTestResults.instance;
    }

    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(projectId.get())
        .setGene(Serializer.serialize(variant.getGene()))
        .build();
    final GrpcExecuteTestResponse response = blockingStub.executeTest(request);
    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      return EmptyTestResults.instance;
    }
    final Path rootPath = config.getTargetProject().rootPath;
    return Serializer.deserialize(rootPath, response.getTestResults());
  }

  @Override
  public void initialize() {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      ProjectZipper.zipProject(config.getTargetProject(), () -> stream);
    } catch (final IOException e) {
      e.printStackTrace();
    }
    final ByteString zip = ByteString.copyFrom(stream.toByteArray());
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .setConfiguration(Serializer.serialize(config))
        .setProject(zip)
        .build();
    final GrpcRegisterProjectResponse response = blockingStub.registerProject(request);
    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      System.exit(1);
      return;
    }
    projectId = Optional.of(response.getProjectId());
  }

  @Override
  public void finish() {
    if (!projectId.isPresent()) {
      return;
    }
    final GrpcUnregisterProjectRequest request = GrpcUnregisterProjectRequest.newBuilder()
        .setProjectId(projectId.get())
        .build();
    final GrpcUnregisterProjectResponse response = blockingStub.unregisterProject(request);
    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      System.exit(1);
    }
    projectId = Optional.empty();
  }


  // 以下テスト用アクセッサ

  void setProjectId(final Integer projectId) {
    this.projectId = Optional.ofNullable(projectId);
  }

  int getProjectId() {
    return projectId.isPresent() ? projectId.get() : -1;
  }
}
