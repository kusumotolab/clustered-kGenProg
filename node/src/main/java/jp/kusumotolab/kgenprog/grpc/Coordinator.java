package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class Coordinator extends KGenProgClusterGrpc.KGenProgClusterImplBase {

  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_FAILED = -1;

  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private final Server server;
  private final AtomicInteger idCounter;
  private final ConcurrentMap<Integer, Project> projectMap;

  public Coordinator(final int port) {
    server = ServerBuilder.forPort(port)
        .addService(this)
        .build();

    idCounter = new AtomicInteger(0);
    projectMap = new ConcurrentHashMap<>();
  }

  public void start() throws IOException, InterruptedException {
    try {
      server.start();
      server.awaitTermination();
    } finally {
      if (server != null) {
        server.shutdown();
      }
    }
  }

  @Override
  public void registerProject(final GrpcRegisterProjectRequest request,
      final StreamObserver<GrpcRegisterProjectResponse> responseObserver) {
    log.info("registerProject request");
    log.debug(request.toString());

    final int projectId = idCounter.getAndIncrement();
    final Configuration config = Serializer.deserialize(request.getConfiguration());
    final Project project = new Project(projectId, config);
    projectMap.put(projectId, project);

    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setProjectId(projectId)
        .setStatus(STATUS_SUCCESS)
        .build();

    responseObserver.onNext(response);
    log.info("registerProject response");
    log.debug(response.toString());
    responseObserver.onCompleted();
  }

  @Override
  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    log.info("executeTest request");
    log.debug(request.toString());

    final GrpcExecuteTestResponse response;
    final Project project = projectMap.get(request.getProjectId());
    if (project == null) {
      response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();

    } else {
      final Gene gene = Serializer.deserialize(request.getGene());
      final TestResults results = project.executeTest(gene);
      response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(STATUS_SUCCESS)
          .setTestResults(Serializer.serialize(results))
          .build();
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("executeTest response");
    log.debug(response.toString());
  }

  @Override
  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");
    log.debug(request.toString());

    final GrpcUnregisterProjectResponse response;
    final Project project = projectMap.remove(request.getProjectId());
    if (project == null) {
      response = GrpcUnregisterProjectResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();

    } else {
      project.unregister();
      response = GrpcUnregisterProjectResponse.newBuilder()
          .setStatus(STATUS_SUCCESS)
          .build();
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("unregisterProject response");
    log.debug(response.toString());
  }
}
