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

  public WorkerSetLogger(final Events events) {
    this.events = events;
    testRequestMap = new HashMap<>();
    queueCount = new HashMap<>();
    testQueueCount = 0;
  }

  public void offerExecuteTest(final ExecuteTestRequest executeTestRequest) {
    events.addEvent(date -> {
      final TestRequestQueue testRequestQueue = updateQueueCount(date, 1, -1, 0);
      return Observable.just(testRequestQueue);
    });
  }

  public void skipExecuteTest(final ExecuteTestRequest executeTestRequest) {
    events.addEvent(date -> {
      final TestRequestQueue testRequestQueue = updateQueueCount(date, -1, -1, 0);
      return Observable.just(testRequestQueue);
    });
  }

  public void startExecuteTest(final ExecuteTestRequest executeTestRequest, final int testRequestId,
      final Worker worker) {
    events.addEvent(date -> {
      final int testId = executeTestRequest.getTestId();
      testRequestMap.put(testRequestId, new TestRequest(testRequestId, testId, date, worker));

      final TestRequestQueue testRequestQueue = updateQueueCount(date, -1, worker.getId(), 1);

      return Observable.just(testRequestQueue);
    });
  }

  public void finishExecuteTest(final ExecuteTestRequest executeTestRequest,
      final int testRequestId, final Worker worker, final GrpcExecuteTestResponse response) {
    events.addEvent(date -> {
      final TestRequest request = testRequestMap.remove(testRequestId);
      request.setResponseTime(date);
      request.success = true;
      request.buildSuccess = !response.getTestResults()
          .getEmpty();

      final TestRequestQueue testRequestQueue = updateQueueCount(date, 0, request.workerId, -1);

      return Observable.just(request, testRequestQueue);
    });
  }

  public void failedExecuteTest(final ExecuteTestRequest executeTestRequest,
      final int testRequestId, final Worker worker) {
    events.addEvent(date -> {
      final TestRequest request = testRequestMap.remove(testRequestId);
      request.setResponseTime(date);
      request.success = false;
      request.buildSuccess = false;

      final TestRequestQueue testRequestQueue = updateQueueCount(date, 1, request.workerId, -1);

      return Observable.just(request, testRequestQueue);
    });
  }

  private TestRequestQueue updateQueueCount(final Instant date, final int coordinatorDiff,
      final int workerId, final int diff) {
    testQueueCount += coordinatorDiff;
    final Integer worker = queueCount.merge(workerId, diff, (a, b) -> a + b);
    return new TestRequestQueue(date, testQueueCount, workerId, worker);
  }

}
