package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import io.reactivex.Observable;
import jp.kusumotolab.kgenprog.coordinator.ExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class WorkerSetLogger {
  private static final WorkerSetLogger workerSetLogger = new WorkerSetLogger(Events.events);
  
  public static WorkerSetLogger getWorkerSetLogger() {
    return workerSetLogger;
  }

  private final Map<Integer, TestRequest> testRequestMap;
  private final Map<Integer, Integer> queueCount;
  private int testQueueCount;
  private final Events events;

  public WorkerSetLogger(Events events) {
    this.events = events;
    testRequestMap = new HashMap<>();
    queueCount = new HashMap<>();
    testQueueCount = 0;
  }

  public void offerExecuteTest(ExecuteTestRequest executeTestRequest) {
    events.addEvent(date -> {
      TestRequestQueue testRequestQueue = updateQueueCount(date, 1, -1, 0);
      return Observable.just(testRequestQueue);
    });
  }

  public void skipExecuteTest(ExecuteTestRequest executeTestRequest) {
    events.addEvent(date -> {
      TestRequestQueue testRequestQueue = updateQueueCount(date, -1, -1, 0);
      return Observable.just(testRequestQueue);
    });
  }

  public void startExecuteTest(ExecuteTestRequest executeTestRequest, final int testRequestId,
      final Worker worker) {
    events.addEvent(date -> {
      int testId = executeTestRequest.getTestId();
      testRequestMap.put(testRequestId, new TestRequest(testRequestId, testId, date, worker));

      TestRequestQueue testRequestQueue = updateQueueCount(date, -1, worker.getId(), 1);

      return Observable.just(testRequestQueue);
    });
  }

  public void finishExecuteTest(ExecuteTestRequest executeTestRequest, final int testRequestId,
      final Worker worker, final GrpcExecuteTestResponse response) {
    events.addEvent(date -> {
      TestRequest request = testRequestMap.remove(testRequestId);
      request.setResponseTime(date);
      request.success = true;
      request.buildSuccess = !response.getTestResults().getEmpty();

      TestRequestQueue testRequestQueue = updateQueueCount(date, 0, request.workerId, -1);

      return Observable.just(request, testRequestQueue);
    });
  }

  public void failedExecuteTest(ExecuteTestRequest executeTestRequest, final int testRequestId,
      final Worker worker) {
    events.addEvent(date -> {
      TestRequest request = testRequestMap.remove(testRequestId);
      request.setResponseTime(date);
      request.success = false;
      request.buildSuccess = false;

      TestRequestQueue testRequestQueue = updateQueueCount(date, 1, request.workerId, -1);

      return Observable.just(request, testRequestQueue);
    });
  }

  private TestRequestQueue updateQueueCount(Instant date, int coordinatorDiff, int workerId,
      int diff) {
    testQueueCount += coordinatorDiff;
    Integer worker = queueCount.merge(workerId, diff, (a, b) -> a + b);
    return new TestRequestQueue(date, testQueueCount, workerId, worker);
  }

}
