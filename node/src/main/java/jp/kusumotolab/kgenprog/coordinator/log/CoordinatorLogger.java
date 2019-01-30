package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import io.reactivex.Observable;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class CoordinatorLogger {

  private static CoordinatorLogger coordinatorLogger = new CoordinatorLogger(Events.events);

  public static CoordinatorLogger getCoordinatorLogger() {
    return coordinatorLogger;
  }

  private final Events events;
  private final Map<Integer, Request> requestMap;
  private final Map<String, Integer> queueCount;

  private CoordinatorLogger(final Events events) {
    this.events = events;
    requestMap = new HashMap<>();
    queueCount = new HashMap<>();
  }


  public void startRequest(final int requestId, final String method, final String clientIp) {
    events.addEvent(time -> {
      requestMap.put(requestId, new Request(requestId, method, time));

      final RequestQueue requestQueue = updateQueueCount(time, method, 1);

      return Observable.just(requestQueue);
    });
  }

  public void registerProject(final int requestId,
      final GrpcRegisterProjectRequest registerProjectRequest,
      final GrpcRegisterProjectResponse registerProjectResponse) {

    events.addEvent(time -> {
      final Request request = requestMap.get(requestId);

      request.status = registerProjectResponse.getStatus();
      request.projectId = registerProjectResponse.getProjectId();

      return Observable.empty();
    });
  }

  public void unregisterProject(final int requestId,
      final GrpcUnregisterProjectResponse unregisterProjectResponse) {
    events.addEvent(date -> {
      final Request request = requestMap.get(requestId);
      request.status = unregisterProjectResponse.getStatus();
      return Observable.empty();
    });
  }

  public void registerWorker(final int requestId, final Worker worker,
      final GrpcRegisterWorkerResponse response) {
    events.addEvent(date -> {
      final Request request = requestMap.get(requestId);

      request.status = response.getStatus();
      request.setWorker(worker);

      return Observable.empty();
    });
  }

  public void getProject(final int requestId, final GrpcGetProjectRequest getProjectRequest,
      final GrpcGetProjectResponse response) {
    events.addEvent(date -> {
      final Request request = requestMap.get(requestId);

      request.projectId = getProjectRequest.getProjectId();
      request.status = response.getStatus();
      return Observable.empty();
    });
  }

  public void error(final int requestId, final Throwable error) {
    events.addEvent(date -> {
      final Request request = requestMap.get(requestId);

      request.errors.add(new ErrorMessage(error));
      return Observable.empty();
    });
  }

  public void finishRequest(final int requestId, final String clientIp) {
    events.addEvent(date -> {
      final Request request = requestMap.remove(requestId);
      request.setResponseTime(date);

      final RequestQueue requestQueue = updateQueueCount(date, request.method, -1);

      return Observable.just(request, requestQueue);
    });
  }

  private RequestQueue updateQueueCount(final Instant date, final String method, final int diff) {
    final Integer update = queueCount.merge(method, diff, (a, b) -> a + b);
    return new RequestQueue(date, method, update);
  }
}
