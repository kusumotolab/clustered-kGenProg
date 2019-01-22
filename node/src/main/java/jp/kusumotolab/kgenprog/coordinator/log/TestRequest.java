package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Duration;
import java.time.Instant;
import jp.kusumotolab.kgenprog.grpc.Worker;

class TestRequest implements EventDocument {

  int testRequestId;
  int testId;
  int workerId;
  String workerName;
  Instant startDate;
  long responseTime;
  boolean success;
  boolean buildSuccess;

  public TestRequest(int testRequestId, int testId, Instant startDate, Worker worker) {
    this.testRequestId = testRequestId;
    this.testId = testId;
    this.startDate = startDate;
    this.workerId = worker.getId();
    this.workerName = worker.getName();
  }

  public void setResponseTime(Instant finishTime) {
    this.responseTime = Duration.between(startDate, finishTime)
        .toMillis();
  }

  @Override
  public String getType() {
    return "testRequest";
  }
}