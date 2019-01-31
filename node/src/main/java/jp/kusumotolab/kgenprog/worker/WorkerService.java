package jp.kusumotolab.kgenprog.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.stub.StreamObserver;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterImplBase;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class WorkerService extends KGenProgClusterImplBase {

  private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

  private final Worker worker;

  public WorkerService(final Worker worker) {
    this.worker = worker;
  }

  @Override
  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    log.info("executeTest request");
    log.debug(request.toString());

    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);
    responseSingle.subscribe(response -> {
      log.info("executeTest response");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }, e -> {
      log.error(e.toString());
      responseObserver.onError(e);
    });

  }

  @Override
  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    log.info("unregisterProject request");

    final Single<GrpcUnregisterProjectResponse> responseSingle = worker.unregisterProject(request);

    responseSingle.subscribe(response -> {
      log.info("unregisterProject response");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

      // EXP-FOR-FSE
      System.exit(0);
      System.exit(0);
    }, e -> {
      log.error(e.toString());
      responseObserver.onError(e);
    });
  }
}
