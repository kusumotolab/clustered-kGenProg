package jp.kusumotolab.kgenprog.coordinator.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.reactivex.Observable;
import jp.kusumotolab.kgenprog.coordinator.ExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class CoordinatorLogger {

  private static CoordinatorLogger coordinatorLogger = new CoordinatorLogger();
  
  public static CoordinatorLogger getCoordinatorLogger() {
    return coordinatorLogger;
  }

  private static final Logger log = LoggerFactory.getLogger(CoordinatorLogger.class);
  private static final Gson gson = createGson();


  @SuppressWarnings("unused")
  private static class Request {

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
  }


  @SuppressWarnings("unused")
  private static class ErrorMessage {

    public String message;
    public String stackTrace;

    public ErrorMessage(Throwable error) {
      message = error.getMessage();

      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      error.printStackTrace(printWriter);
      printWriter.flush();
      stackTrace = stringWriter.toString();
    }
  }

  Events events;
  Map<Integer, Request> requestMap;

  private CoordinatorLogger() {
    events = new Events();
    requestMap = new HashMap<>();
    events.getEventObservable()
        .subscribe(json -> log.info(gson.toJson(json)));
  }


  public void startRequest(final int requestId, final String method, String clientIp) {
    events.addEvent(time -> {
      requestMap.put(requestId, new Request(requestId, method, time));

      return Observable.empty();
    });
  }

  public void registerProject(final int requestId,
      final GrpcRegisterProjectRequest registerProjectRequest,
      final GrpcRegisterProjectResponse registerProjectResponse) {

    events.addEvent(time -> {
      Request request = requestMap.get(requestId);

      request.status = registerProjectResponse.getStatus();
      request.projectId = registerProjectResponse.getProjectId();

      return Observable.empty();
    });
  }

  public void skipExecuteTest(ExecuteTestRequest executeTestRequest) {
    events.addEvent(date -> {
      Request request = requestMap.get(executeTestRequest.getRequestId());
      request.testId = executeTestRequest.getTestId();
      request.testSuccess = false;

      return Observable.empty();
    });
  }

  public void startExecuteTest(ExecuteTestRequest request, final Worker worker) {
    /*
     * final JsonObject json = new JsonObject();
     * 
     * json.addProperty(EVENT, "startExecuteTest"); json.addProperty(REQUEST_ID, requestId);
     * json.addProperty(TEST_ID, testId); json.add(WORKER, toJson(worker));
     * json.addProperty(PROJECT_ID, executeTestRequest.getProjectId());
     */

  }

  public void finishExecuteTest(ExecuteTestRequest executeTestRequest, final Worker worker,
      final GrpcExecuteTestResponse response) {

    events.addEvent(date -> {
      Request request = requestMap.get(executeTestRequest.getRequestId());
      request.testId = executeTestRequest.getTestId();
      request.setWorker(worker);
      request.testSuccess = !response.getTestResults()
          .getEmpty();

      return Observable.empty();
    });
  }

  public void failedExecuteTest(ExecuteTestRequest executeTestRequest, final Worker worker) {

  }

  public void unregisterProject(int requestId,
      GrpcUnregisterProjectResponse unregisterProjectResponse) {
    events.addEvent(date -> {
      Request request = requestMap.get(requestId);
      request.status = unregisterProjectResponse.getStatus();
      return Observable.empty();
    });
  }

  public void registerWorker(final int requestId, final Worker worker,
      final GrpcRegisterWorkerResponse response) {
    events.addEvent(date -> {
      Request request = requestMap.get(requestId);

      request.status = response.getStatus();
      request.setWorker(worker);

      return Observable.empty();
    });
  }

  public void getProject(final int requestId, final GrpcGetProjectRequest getProjectRequest,
      final GrpcGetProjectResponse response) {
    events.addEvent(date -> {
      Request request = requestMap.get(requestId);

      request.projectId = getProjectRequest.getProjectId();
      request.status = response.getStatus();
      return Observable.empty();
    });
  }

  public void error(final int requestId, final Throwable error) {
    events.addEvent(date -> {
      Request request = requestMap.get(requestId);

      request.errors.add(new ErrorMessage(error));
      return Observable.empty();
    });
  }

  public void finishRequest(final int requestId, final String clientIp) {
    events.addEvent(date -> {
      Request request = requestMap.get(requestId);
      request.setResponseTime(date);
      JsonElement json = gson.toJsonTree(request);
      return Observable.just(json);
    });
  }

  private static Gson createGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Instant.class,
        (JsonSerializer<Instant>) ((src, typeOfSrc, context) -> {
          ZonedDateTime zonedDateTime = src.atZone(ZoneId.systemDefault());
          return new JsonPrimitive(zonedDateTime.toString());
        }));

    return gsonBuilder.create();
  }
}
