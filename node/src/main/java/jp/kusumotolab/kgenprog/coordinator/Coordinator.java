package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
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

public class Coordinator {

  public static final int STATUS_SUCCESS = 0;
  public static final int STATUS_FAILED = -1;
  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

  private final Server server;
  private final AtomicInteger idCounter;
  private final LoadBalancer loadBalancer = new LoadBalancer();
  private final List<ServerServiceDefinition> services = new ArrayList<>();
  private final ConcurrentMap<Integer, ByteString> binaryMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, GrpcConfiguration> configurationMap =
      new ConcurrentHashMap<>();
  private final ClientHostNameCaptor clientHostNameCaptor = new ClientHostNameCaptor();

  private final Subject<Pair<GrpcExecuteTestRequest, StreamObserver<GrpcExecuteTestResponse>>> subject;

  public Coordinator(final ClusterConfiguration config) {
    final BehaviorSubject<Pair<GrpcExecuteTestRequest, StreamObserver<GrpcExecuteTestResponse>>> behaviorSubject = BehaviorSubject.create();
    subject = behaviorSubject.toSerialized();

    server = ServerBuilder.forPort(config.getPort())
        .addService(new KGenProgCluster(this))
        .addService(
            ServerInterceptors.intercept(new CoordinatorService(this), clientHostNameCaptor))
        .maxInboundMessageSize(Integer.MAX_VALUE)
        .build();

    services.addAll(server.getServices());

    idCounter = new AtomicInteger(0);

    final Object nullObject = new Object();
    Observable.zip(this.subject, loadBalancer.getHotWorkerObserver(), ((pair, worker) -> {
      this.executeTest(pair.getLeft(), pair.getValue(), worker);
      return nullObject;
    }))
        .subscribe(e -> {
        }, e -> log.error(e.toString()));

  }

  private void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver, final Worker worker) {
    log.info("executeTest request");
    log.debug(request.toString());

    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);
    responseSingle.subscribeOn(Schedulers.computation()).subscribe(response -> {
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      loadBalancer.finish(worker);
      log.info("executeTest response");
      log.debug(response.toString());

    }, error -> {
      final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(STATUS_FAILED)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
      loadBalancer.finish(worker);
      log.info("executeTest response");
      log.debug(response.toString());
    });
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
        .setStatus(STATUS_SUCCESS)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
    log.info("registerProject response");
    log.debug(response.toString());
  }

  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    subject.onNext(new ImmutablePair<>(request, responseObserver));
  }

  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");
    log.debug(request.toString());

    for (final Worker worker : loadBalancer.getWorkerList()) {
      if (worker == null) {
        // FIXME: 2019/01/18 なぜかnullになる
        continue;
      }
      worker.unregisterProject(request)
          .subscribe(r -> {
          }, e -> log.error(e.toString()));
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
    final String hostName = clientHostNameCaptor.getHostName();
    log.info("Client host name: " + hostName);

    final Worker remoteWorker = createWorker(hostName, request.getPort());
    addWorkerToLoadBalancer(remoteWorker);

    final GrpcRegisterWorkerResponse response = GrpcRegisterWorkerResponse.newBuilder()
        .setStatus(STATUS_SUCCESS)
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


  // 以下はテスト用メソッド
  protected List<ServerServiceDefinition> getServices() {
    return services;
  }

  protected Worker createWorker(final String name, final int port) {
    return new RemoteWorker(name, port);
  }

  protected void addWorkerToLoadBalancer(final Worker worker) {
    loadBalancer.addWorker(worker);
  }
}
