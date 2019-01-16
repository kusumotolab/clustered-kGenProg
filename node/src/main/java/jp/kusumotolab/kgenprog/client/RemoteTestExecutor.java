package jp.kusumotolab.kgenprog.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
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
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterFutureStub;
import jp.kusumotolab.kgenprog.grpc.ProjectZipper;
import jp.kusumotolab.kgenprog.grpc.Serializer;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class RemoteTestExecutor implements TestExecutor {

  private static final Logger log = LoggerFactory.getLogger(RemoteTestExecutor.class);

  private final KGenProgClusterBlockingStub blockingStub;
  private final KGenProgClusterFutureStub futureStub;
  private final Configuration config;
  private Optional<Integer> projectId = Optional.empty();
  private ListeningExecutorService pool;

  public RemoteTestExecutor(final Configuration config, final String name, final int port) {
    this.config = config;
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(name, port)
        .usePlaintext()
        .build();
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
    futureStub = KGenProgClusterGrpc.newFutureStub(managedChannel);
    pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
  }

  public RemoteTestExecutor(final Configuration config, final ManagedChannel managedChannel) {
    this.config = config;
    blockingStub = KGenProgClusterGrpc.newBlockingStub(managedChannel);
    futureStub = KGenProgClusterGrpc.newFutureStub(managedChannel);
  }

  @Override
  public Single<TestResults> execAsync(final Single<Variant> variantSingle) {
    if (!projectId.isPresent()) {
      log.error("project id is not present");
      return Single.just(EmptyTestResults.instance);
    }


    final Single<ListenableFuture<GrpcExecuteTestResponse>> requestSingle =
        variantSingle.map(variant -> {

          final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
              .setProjectId(projectId.get())
              .setGene(Serializer.serialize(variant.getGene()))
              .build();
          log.debug("executeTest request");
          log.debug(request.toString());


          final ListenableFuture<GrpcExecuteTestResponse> listenableFuture =
              futureStub.executeTest(request);
          return listenableFuture;
        });

    return requestSingle.flatMap(future -> {
      final Single<TestResults> results = Single.create(subscriber -> {
        future.addListener(() -> {
          GrpcExecuteTestResponse response;
          try {
            response = future.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }

          if (response.getStatus() == Coordinator.STATUS_FAILED) {
            log.error("failed to executeTest");
            subscriber.onSuccess(EmptyTestResults.instance);
            return;
          }

          final Path rootPath = config.getTargetProject().rootPath;
          final TestResults testResults =
              Serializer.deserialize(rootPath, response.getTestResults());
          subscriber.onSuccess(testResults);
          log.debug("executeTest response");
          log.debug(response.toString());
        }, pool);
      });
      return results;

    });
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

  @Override
  public TestResults exec(final Variant variant) {
    throw new UnsupportedOperationException();
  }
}
