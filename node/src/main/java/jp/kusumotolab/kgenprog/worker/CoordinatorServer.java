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

public class CoordinatorServer extends KGenProgClusterImplBase {

  private static final Logger log = LoggerFactory.getLogger(CoordinatorServer.class);

  private final Worker worker;

  public CoordinatorServer(final Worker worker) {
    this.worker = worker;
  }

  @Override
  public void executeTest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> responseObserver) {
    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);
    final GrpcExecuteTestResponse response = responseSingle.blockingGet();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void unregisterProject(final GrpcUnregisterProjectRequest request,
      final StreamObserver<GrpcUnregisterProjectResponse> responseObserver) {
    final Single<GrpcUnregisterProjectResponse> responseSingle = worker.unregisterProject(request);
    final GrpcUnregisterProjectResponse response = responseSingle.blockingGet();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
