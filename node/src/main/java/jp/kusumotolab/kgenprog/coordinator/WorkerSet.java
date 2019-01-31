package jp.kusumotolab.kgenprog.coordinator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import io.grpc.stub.StreamObserver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import jp.kusumotolab.kgenprog.coordinator.log.CoordinatorLogger;
import jp.kusumotolab.kgenprog.coordinator.log.WorkerSetLogger;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class WorkerSet {
  private static final WorkerSetLogger workerSetLogger = WorkerSetLogger.getWorkerSetLogger();
  private static final CoordinatorLogger coordinatorLogger =
      CoordinatorLogger.getCoordinatorLogger();

  private final ConcurrentMap<Worker, Worker> workerMap = new ConcurrentHashMap<>();
  private final Subject<Worker> workerSubject;
  private final Subject<ExecuteTestRequest> testRequestSubject;
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private final RequestValidator requestValidator = new RequestValidator();
  private final AtomicInteger testRequestIdCounter = new AtomicInteger(0);

  public WorkerSet() {
    final BehaviorSubject<Worker> workerBehaviorSubject = BehaviorSubject.create();
    this.workerSubject = workerBehaviorSubject.toSerialized();

    final BehaviorSubject<ExecuteTestRequest> testRequestBehaviorSubject = BehaviorSubject.create();
    this.testRequestSubject = testRequestBehaviorSubject.toSerialized();

    // 2つのSubjectを購読
    final Object nullObject = new Object();
    Observable.zip(testRequestSubject, workerSubject, (request, worker) -> {
      executeTest(request, worker);
      return nullObject;
    })
        .subscribe(o -> {
        }, error ->  coordinatorLogger.error(-1, error));
  }

  public void addWorker(final Worker worker) {
    workerMap.putIfAbsent(worker, worker);
    workerSubject.onNext(worker);
  }

  public void executeTest(final ExecuteTestRequest testRequest) {
    workerSetLogger.offerExecuteTest(testRequest);
    testRequestSubject.onNext(testRequest);
  }

  protected void executeTest(final ExecuteTestRequest testRequest, final Worker worker) {
    final GrpcExecuteTestRequest request = testRequest.getRequest();
    final StreamObserver<GrpcExecuteTestResponse> responseObserver =
        testRequest.getStreamObserver();

    if (!requestValidator.validate(testRequest)) {
      workerSetLogger.skipExecuteTest(testRequest);
      workerSubject.onNext(worker);
      return;
    }
    
    final int testReuquestId = testRequestIdCounter.getAndIncrement();

    workerSetLogger.startExecuteTest(testRequest, testReuquestId, worker);

    final Single<GrpcExecuteTestResponse> responseSingle = worker.executeTest(request);
    responseSingle.subscribeOn(Schedulers.from(getExecutorService()))
        .subscribe(response -> {
          workerSubject.onNext(worker);
          workerSetLogger.finishExecuteTest(testRequest, testReuquestId, worker, response);

          try {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
          } catch (final RuntimeException e) {
            requestValidator.addInvalidateRequest(testRequest);
            coordinatorLogger.error(testRequest.getRequestId(), e);

            // EXP-FOR-FSE
            System.exit(0);
          }
        }, error -> {
          // workerとの通信が途絶えるとここに入る
          workerSetLogger.failedExecuteTest(testRequest, testReuquestId, worker);
          testRequestSubject.onNext(testRequest);
          remove(worker);
        });
  }

  private void remove(final Worker worker) {
    worker.finish();
    workerMap.remove(worker);
  }

  public void unregister(final int requestId, final GrpcUnregisterProjectRequest request) {
    for (final Worker worker : workerMap.values()) {
      worker.unregisterProject(request)
          .subscribe(r -> {
          }, e -> coordinatorLogger.error(requestId, e));
    }
  }

  public Collection<Worker> getAllWorker() {
    return workerMap.values();
  }

  // テストのために切り出し
  protected ExecutorService getExecutorService() {
    return executorService;
  }
}
