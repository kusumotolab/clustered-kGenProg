package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import jp.kusumotolab.kgenprog.coordinator.log.CoordinatorLogger;
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

  private static final CoordinatorLogger coordinatorLogger =
      CoordinatorLogger.getCoordinatorLogger();

  private final Server server;
  private final AtomicInteger projectIdCounter;
  private final AtomicInteger workerIdCounter;
  private final AtomicInteger testIdCounter;
  private final WorkerSet workerSet = new WorkerSet();
  private final List<ServerServiceDefinition> services = new ArrayList<>();
  private final ConcurrentMap<Integer, ByteString> binaryMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, GrpcConfiguration> configurationMap =
      new ConcurrentHashMap<>();
  private final CoordinatorInterceptor interceptor = new CoordinatorInterceptor();

  public Coordinator(final ClusterConfiguration config) {
    server = ServerBuilder.forPort(config.getPort())
        .addService(ServerInterceptors.intercept(new KGenProgCluster(this), interceptor))
        .addService(ServerInterceptors.intercept(new CoordinatorService(this), interceptor))
        .maxInboundMessageSize(Integer.MAX_VALUE)
        .build();

    services.addAll(server.getServices());

    projectIdCounter = new AtomicInteger(0);
    workerIdCounter = new AtomicInteger(0);
    testIdCounter = new AtomicInteger(0);
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
    final int requestId = interceptor.getRequestId();
    final int projectId = projectIdCounter.getAndIncrement();

    binaryMap.put(projectId, request.getProject());
    configurationMap.put(projectId, request.getConfiguration());
    distributeAllWorker(projectId);

    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setProjectId(projectId)
        .setStatus(GrpcStatus.SUCCESS)
        .build();

    coordinatorLogger.registerProject(requestId, request, response);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    final int requestId = interceptor.getRequestId();
    final int testId = testIdCounter.getAndIncrement();
    final String hostName = interceptor.getHostName();
    final int port = interceptor.getPort();
    workerSet.executeTest(
        new ExecuteTestRequest(request, responseObserver, hostName, port, requestId, testId));
  }

  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    final int requestId = interceptor.getRequestId();

    workerSet.unregister(requestId, request);

    binaryMap.remove(request.getProjectId());
    configurationMap.remove(request.getProjectId());

    final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
        .setStatus(GrpcStatus.SUCCESS)
        .build();

    coordinatorLogger.unregisterProject(requestId, response);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  public void registerWorker(final GrpcRegisterWorkerRequest request,
      final StreamObserver<GrpcRegisterWorkerResponse> responseObserver) {
    final int requestId = interceptor.getRequestId();
    final int workerId = workerIdCounter.getAndIncrement();
    final String hostName = interceptor.getHostName();
    final int port = request.getPort();

    final Worker remoteWorker = createWorker(workerId, hostName, port);
    addWorker(remoteWorker);

    final GrpcRegisterWorkerResponse response = GrpcRegisterWorkerResponse.newBuilder()
        .setStatus(GrpcStatus.SUCCESS)
        .build();

    coordinatorLogger.registerWorker(requestId, remoteWorker, response);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  public void getProject(final GrpcGetProjectRequest request,
      final StreamObserver<GrpcGetProjectResponse> responseObserver) {
    final int requestId = interceptor.getRequestId();
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

    coordinatorLogger.getProject(requestId, request, response);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // 以下はテスト用メソッド
  protected List<ServerServiceDefinition> getServices() {
    return services;
  }

  protected Worker createWorker(final int workerId, final String name, final int port) {
    return new RemoteWorker(workerId, name, port);
  }

  protected void addWorker(final Worker worker) {
    for (final Integer projectId : binaryMap.keySet()) {
      final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
          .setProjectId(projectId)
          .build();
      worker.executeTest(request)
          .blockingGet();
    }
    workerSet.addWorker(worker);
  }

  private void distributeAllWorker(final int projectId) {
    final Collection<Worker> allWorker = workerSet.getAllWorker();
    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(projectId)
        .build();
    final List<Completable> completableList = allWorker.stream()
        .map(e -> e.executeTest(request)
            .subscribeOn(Schedulers.from(workerSet.getExecutorService())))
        .map(Completable::fromSingle)
        .collect(Collectors.toList());
    Completable.merge(completableList)
        .blockingGet();
  }
}
