package jp.kusumotolab.kgenprog.coordinator.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class CoordinatorLogger {

  private static final Logger log = LoggerFactory.getLogger(CoordinatorLogger.class);
  private static final Gson gson = createGson();

  public final static String EVENT = "event";
  public final static String REQUEST_ID = "request_id";
  public final static String TEST_ID = "test_id";
  public final static String WORKER = "worker";
  public final static String CLIENT_IP = "client_ip";
  public final static String ID = "id";
  public final static String NAME = "name";
  public final static String PROJECT_ID = "project_id";
  public final static String TEST_SUCCESS = "test_success";
  public final static String STATUS = "status";
  public final static String ERROR_MESSAGE = "error_message";
  public final static String STACKTRACE = "stacktrace";
  public final static String TIME = "time";

  public static void request(final int requestId, final String clientIp) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "request");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(CLIENT_IP, clientIp);

    storeLog(json);
  }

  public static void registerProject(final int requestId, final GrpcRegisterProjectRequest request,
      final GrpcRegisterProjectResponse response) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "registerProject");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(STATUS, response.getStatus());
    json.addProperty(PROJECT_ID, response.getProjectId());

    storeLog(json);
  }

  public static void startExecuteTest(final int requestId, final int testId, final Worker worker,
      final GrpcExecuteTestRequest request) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "startExecuteTest");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(TEST_ID, testId);
    json.add(WORKER, toJson(worker));
    json.addProperty(PROJECT_ID, request.getProjectId());


    storeLog(json);
  }

  public static void finishExecuteTest(final int requestId, final int testId, final Worker worker,
      final GrpcExecuteTestResponse response) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "finishExecuteTest");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(TEST_ID, testId);
    json.add(WORKER, toJson(worker));
    json.addProperty(TEST_SUCCESS, !response.getTestResults()
        .getEmpty());

    storeLog(json);
  }

  public static void unregisterProject(int requestId, GrpcUnregisterProjectResponse response) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "unregisterProject");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(STATUS, response.getStatus());

    storeLog(json);
  }

  public static void registerWorker(final int requestId, final Worker worker,
      final GrpcRegisterWorkerResponse response) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "registerWorker");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(STATUS, response.getStatus());
    json.add(WORKER, toJson(worker));

    storeLog(json);
  }

  public static void getProject(final int requestId, final GrpcGetProjectRequest request,
      final GrpcGetProjectResponse response) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "getProject");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(PROJECT_ID, request.getProjectId());
    json.addProperty(STATUS, response.getStatus());

    storeLog(json);
  }

  public static void error(final int requestId, final Throwable error) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "error");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(ERROR_MESSAGE, error.getMessage());
    json.addProperty(STACKTRACE, stackTraceToString(error));

    storeLog(json);
  }

  public static void close(final int requestId, final String clientIp) {
    final JsonObject json = new JsonObject();

    setTime(json);
    json.addProperty(EVENT, "close");
    json.addProperty(REQUEST_ID, requestId);
    json.addProperty(CLIENT_IP, clientIp);

    storeLog(json);
  }

  private static void storeLog(final JsonElement element) {
    log.info(gson.toJson(element));
  }

  private static void setTime(JsonObject object) {
    object.addProperty(TIME, Instant.now()
        .getEpochSecond());
  }

  private static JsonElement toJson(Worker worker) {
    final JsonObject json = new JsonObject();

    json.addProperty(ID, worker.getId());
    json.addProperty(NAME, worker.getName());

    return json;
  }

  private static Gson createGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    return gsonBuilder.create();
  }

  private static String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }

}
