package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jp.kusumotolab.kgenprog.grpc.Worker;

@SuppressWarnings("unused") class Request implements EventDocument {

  public int requestId;
  public String method;
  public Instant startDate;
  public int projectId;
  public int status;
  public int workerId;
  public String workerName;
  public long responseTime;

  public List<ErrorMessage> errors;
  public int testId;
  public boolean testSuccess;

  public Request(int requestId, String method, Instant startDate) {
    this.requestId = requestId;
    this.method = method;
    this.startDate = startDate;

    errors = new ArrayList<>(0);
  }

  public void setResponseTime(Instant finishTime) {
    this.responseTime = Duration.between(startDate, finishTime)
        .toMillis();
  }

  public void setWorker(Worker worker) {
    this.workerId = worker.getId();
    this.workerName = worker.getName();
  }

  @Override
  public String getType() {
    return "request";
  }
}