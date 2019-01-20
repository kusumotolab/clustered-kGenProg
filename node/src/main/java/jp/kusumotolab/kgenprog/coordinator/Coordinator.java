package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
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
import jp.kusumotolab.kgenprog.grpc.GrpcStatus;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class Coordinator {

  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private final Server server;
  private final AtomicInteger idCounter;
  private final WorkerSet workerSet = new WorkerSet();
  private final List<ServerServiceDefinition> services = new ArrayList<>();
  private final ConcurrentMap<Integer, ByteString> binaryMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, GrpcConfiguration> configurationMap =
      new ConcurrentHashMap<>();
  private final ClientHostNameCaptor clientHostNameCaptor = new ClientHostNameCaptor();

  public Coordinator(final ClusterConfiguration config) {
    server = ServerBuilder.forPort(config.getPort())
        .addService(new KGenProgCluster(this))
        .addService(
            ServerInterceptors.intercept(new CoordinatorService(this), clientHostNameCaptor))
        .maxInboundMessageSize(Integer.MAX_VALUE)
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

  // 以下，各Serviceから呼び出されるメソッド
  public void registerProject(final GrpcRegisterProjectRequest request,
      final StreamObserver<GrpcRegisterProjectResponse> responseObserver) {
    log.info("registerProject request");
    log.debug(request.toString());

    final int projectId = idCounter.getAndIncrement();
    binaryMap.put(projectId, request.getProject());
    configurationMap.put(projectId, request.getConfiguration());

    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setProjectId(projectId)
        .setStatus(GrpcStatus.SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("registerProject response");
    log.debug(response.toString());
  }

  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    workerSet.executeTest(new ExecuteTestRequest(request, responseObserver));
  }

  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");
    log.debug(request.toString());

    workerSet.unregister(request);

    binaryMap.remove(request.getProjectId());
    configurationMap.remove(request.getProjectId());

    final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
        .setStatus(GrpcStatus.SUCCESS)
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
    final String hostName = clientHostNameCaptor.getHostName();
    log.info("Client host name: " + hostName);

    final Worker remoteWorker = createWorker(hostName, request.getPort());
    addWorker(remoteWorker);

    final GrpcRegisterWorkerResponse response = GrpcRegisterWorkerResponse.newBuilder()
        .setStatus(GrpcStatus.SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();

    log.info("registerWorker response");
    log.debug(response.toString());

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
          .setStatus(GrpcStatus.FAILED)
          .build();
    } else {
      response = GrpcGetProjectResponse.newBuilder()
          .setConfiguration(configuration)
          .setProject(byteString)
          .setStatus(GrpcStatus.SUCCESS)
          .build();
    }
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("getProject response");
    log.debug(response.toString());
  }


  // 以下はテスト用メソッド
  protected List<ServerServiceDefinition> getServices() {
    return services;
  }

  protected Worker createWorker(final String name, final int port) {
    return new RemoteWorker(name, port);
  }

  protected void addWorker(final Worker worker) {
    workerSet.addWorker(worker);
  }
}
