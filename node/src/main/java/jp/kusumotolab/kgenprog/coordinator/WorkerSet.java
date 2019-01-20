package jp.kusumotolab.kgenprog.coordinator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.stub.StreamObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcStatus;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class WorkerSet {

  private static final Logger log
       = LoggerFactory.getLogger(WorkerSet.class);

  private final ConcurrentMap<Worker, Worker> workerMap = new ConcurrentHashMap<>();
  private final Subject<Worker> workerSubject;
  private final Subject<ExecuteTestRequest> testRequestSubject;
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  public WorkerSet() {
    final BehaviorSubject<Worker> workerBehaviorSubject= BehaviorSubject.create();
    this.workerSubject = workerBehaviorSubject.toSerialized();

    final BehaviorSubject<ExecuteTestRequest> testRequestBehaviorSubject = BehaviorSubject.create();
    this.testRequestSubject = testRequestBehaviorSubject.toSerialized();

    // 2つのSubjectを購読
    final Object nullObject = new Object();
    Observable.zip(testRequestSubject, workerSubject, (request, worker) -> {
      executeTest(request, worker);
      return nullObject;
    }).subscribe(o -> {}, error -> log.error(error.toString()));
  }

  public void addWorker(final Worker worker) {
    workerMap.putIfAbsent(worker, worker);
    workerSubject.onNext(worker);
  }

  public void executeTest(final ExecuteTestRequest testRequest) {
    testRequestSubject.onNext(testRequest);
  }

  protected void executeTest(final ExecuteTestRequest testRequest, final Worker worker) {
    final GrpcExecuteTestRequest request = testRequest.getRequest();
    final StreamObserver<GrpcExecuteTestResponse> responseObserver= testRequest.getStreamObserver();
    log.info("executeTest request");
    log.debug(request.toString());

    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);
    responseSingle.subscribeOn(Schedulers.from(getExecutorService()))
        .subscribe(response -> {
          responseObserver.onNext(response);
          responseObserver.onCompleted();
          log.info("executeTest response");
          log.debug(response.toString());
          workerSubject.onNext(worker);
        }, error -> {
          final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
              .setStatus(GrpcStatus.FAILED)
              .build();
          responseObserver.onNext(response);
          responseObserver.onCompleted();
          log.info("executeTest response");
          log.debug(response.toString());
          workerSubject.onNext(worker);
        });
  }

  public void unregister(final GrpcUnregisterProjectRequest request) {
    for (final Worker worker : workerMap.values()) {
      worker.unregisterProject(request)
          .subscribe(r -> {
          }, e -> log.error(e.toString()));
    }
  }

  // テストのために切り出し
  protected ExecutorService getExecutorService() {
    return executorService;
  }
}
