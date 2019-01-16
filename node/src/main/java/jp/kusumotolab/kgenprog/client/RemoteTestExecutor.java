package jp.kusumotolab.kgenprog.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.coordinator.Coordinator;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.grpc.GrpcConfiguration;
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
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class RemoteTestExecutor implements TestExecutor {

  private static final Logger log = LoggerFactory.getLogger(RemoteTestExecutor.class);

  private final KGenProgClusterBlockingStub blockingStub;
  private final Configuration config;
  private Optional<Integer> projectId = Optional.empty();

  public RemoteTestExecutor(final Configuration config, final String name, final int port) {
    this.config = config;
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .keepAliveTime(60, TimeUnit.SECONDS)
        .maxInboundMessageSize(Integer.MAX_VALUE)
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
      log.error("project id is not present");
      return EmptyTestResults.instance;
    }

    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(projectId.get())
        .setGene(Serializer.serialize(variant.getGene()))
        .build();
    log.debug("executeTest request");
    log.debug(request.toString());

    final GrpcExecuteTestResponse response = blockingStub.executeTest(request);
    log.debug("executeTest response");
    log.debug(response.toString());

    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      log.error("failed to executeTest");
      return EmptyTestResults.instance;
    }
    final Path rootPath = config.getTargetProject().rootPath;
    return Serializer.deserialize(rootPath, response.getTestResults());
  }

  @Override
  public void initialize() {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    final TargetProject targetProject;
    try {
      targetProject = ProjectZipper.zipProject(config.getTargetProject(), () -> stream);
    } catch (final IOException e) {
      log.error("failed to zip project", e);
      throw new RuntimeException("failed to zip project");
    }

    final GrpcConfiguration configuration = Serializer
        .updateConfiguration(Serializer.serialize(config)
            .toBuilder(), targetProject)
        .build();

    final ByteString zip = ByteString.copyFrom(stream.toByteArray());
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .setConfiguration(configuration)
        .setProject(zip)
        .build();
    log.debug("registerProject request");
    log.debug(request.toString());

    final GrpcRegisterProjectResponse response = blockingStub.registerProject(request);
    log.debug("registerProject response");
    log.debug(response.toString());

    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      log.error("failed to register project");
      throw new RuntimeException("failed to register project");
    }
    projectId = Optional.of(response.getProjectId());
  }

  @Override
  public void finish() {
    if (!projectId.isPresent()) {
      log.error("project id is not present");
      return;
    }
    final GrpcUnregisterProjectRequest request = GrpcUnregisterProjectRequest.newBuilder()
        .setProjectId(projectId.get())
        .build();
    log.debug("unregisterProject request");
    log.debug(request.toString());

    final GrpcUnregisterProjectResponse response = blockingStub.unregisterProject(request);
    log.debug("unregisterProject response");
    log.debug(response.toString());

    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      log.error("failed to unregister project");
    }
    projectId = Optional.empty();
  }


  // 以下テスト用アクセッサ

  void setProjectId(final Integer projectId) {
    this.projectId = Optional.ofNullable(projectId);
  }

  Optional<Integer> getProjectId() {
    return projectId;
  }
}
