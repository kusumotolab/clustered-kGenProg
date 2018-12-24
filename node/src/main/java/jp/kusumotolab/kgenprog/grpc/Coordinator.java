package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.Configuration;

public class Coordinator extends KGenProgClusterGrpc.KGenProgClusterImplBase {

  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_FAILED = -1;
  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private final Server server;
  private final AtomicInteger idCounter;
  private final Worker worker;

  public Coordinator(final int port, final Worker worker) {
    this.worker = worker;
    
    server = ServerBuilder.forPort(port)
        .addService(this)
        .build();

    idCounter = new AtomicInteger(0);
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
    final Single<GrpcRegisterProjectResponse> responseSingle =
        worker.registerProject(request, projectId);

    responseSingle.subscribe(response -> {
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("registerProject response");
      log.debug(response.toString());

    }, error -> {
      final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
          .setProjectId(projectId)
          .setStatus(STATUS_FAILED)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("registerProject response");
      log.debug(response.toString());
    });
  }

  @Override
  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    log.info("executeTest request");
    log.debug(request.toString());

    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);

    responseSingle.subscribe(response -> {
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("executeTest response");
      log.debug(response.toString());

    }, error -> {
      final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("executeTest response");
      log.debug(response.toString());
    });
  }

  @Override
  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");
    log.debug(request.toString());

    final Single<GrpcUnregisterProjectResponse> responseSingle = worker.unregisterProject(request);

    responseSingle.subscribe(response -> {
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("unregisterProject response");
      log.debug(response.toString());

    }, error -> {
      final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("unregisterProject response");
      log.debug(response.toString());
    });
  }

  /**
   * method for test
   */
  protected Project createProject(final int projectId, final Configuration config) {
    return new Project(projectId, config);
  }
}
