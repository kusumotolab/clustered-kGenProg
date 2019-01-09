package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.GrpcConfiguration;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;
import jp.kusumotolab.kgenprog.coordinator.worker.RemoteWorker;

public class Coordinator {

  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_FAILED = -1;
  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private final Server server;
  private final AtomicInteger idCounter;
  private final LoadBalancer loadBalancer = new LoadBalancer();
  private final List<ServerServiceDefinition> services = new ArrayList<>();
  private final Map<Integer, ByteString> binaryMap = new HashMap<>();
  private final Map<Integer, GrpcConfiguration> configurationMap = new HashMap<>();

  public Coordinator(final ClusterConfiguration config) {
    server = ServerBuilder.forPort(config.getPort())
        .addService(new ClientServer(this))
        .addService(new WorkerServer(this))
        .build();

    services.addAll(server.getServices());

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

  public void registerProject(final GrpcRegisterProjectRequest request,
      final StreamObserver<GrpcRegisterProjectResponse> responseObserver) {
    log.info("registerProject request");
    log.debug(request.toString());

    final int projectId = idCounter.getAndIncrement();
    binaryMap.put(projectId, request.getProject());
    configurationMap.put(projectId, request.getConfiguration());

    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setProjectId(projectId)
        .setStatus(STATUS_SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("registerProject response");
    log.debug(response.toString());
  }

  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    log.info("executeTest request");
    log.debug(request.toString());

    final Worker worker = loadBalancer.getWorker();
    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);

    responseSingle.subscribe(response -> {
      loadBalancer.finish(worker);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("executeTest response");
      log.debug(response.toString());

    }, error -> {
      loadBalancer.finish(worker);
      final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      log.info("executeTest response");
      log.debug(response.toString());
    });
  }

  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");
    log.debug(request.toString());

    for (final Worker worker : loadBalancer.getWorkerList()) {
      worker.unregisterProject(request).subscribe(r -> { }, e -> log.error(e.toString()));
    }

    binaryMap.remove(request.getProjectId());
    configurationMap.remove(request.getProjectId());

    final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
        .setStatus(STATUS_SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("unregisterProject response");
    log.debug(response.toString());
  }

  public void registerWorker(final GrpcRegisterWorkerRequest request,
      final StreamObserver<GrpcRegisterWorkerResponse> responseObserver) {
    log.info("registerWorker request");
    log.debug(request.toString());

    final RemoteWorker remoteWorker = new RemoteWorker(request.getHost(), request.getPort());
    loadBalancer.addWorker(remoteWorker);

    final GrpcRegisterWorkerResponse response = GrpcRegisterWorkerResponse.newBuilder()
        .setStatus(STATUS_SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  public void getProject(final GrpcGetProjectRequest request,
      final StreamObserver<GrpcGetProjectResponse> responseObserver) {
    log.info("getProject request");
    log.debug(request.toString());

    final int projectId = request.getProjectId();
    final ByteString byteString = binaryMap.get(projectId);
    final GrpcConfiguration configuration = configurationMap.get(projectId);
    final GrpcGetProjectResponse response;
    if (byteString == null || configuration == null) {
      response = GrpcGetProjectResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();
    } else {
      response = GrpcGetProjectResponse.newBuilder()
          .setConfiguration(configuration)
          .setProject(byteString)
          .setStatus(STATUS_SUCCESS)
          .build();
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("getProject response");
    log.debug(response.toString());
  }

  // テスト用
  public List<ServerServiceDefinition> getServices() {
    return services;
  }
}
