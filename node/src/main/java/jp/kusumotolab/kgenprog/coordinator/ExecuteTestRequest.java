package jp.kusumotolab.kgenprog.coordinator;

import java.time.Instant;
import io.grpc.stub.StreamObserver;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;

public class ExecuteTestRequest {

  private final GrpcExecuteTestRequest request;
  private final StreamObserver<GrpcExecuteTestResponse> streamObserver;
  private final Instant time;
  private final String senderName;
  private final int senderPort;
  private final int requestId;
  private final int testId;

  public ExecuteTestRequest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> streamObserver, final String senderName,
      final int senderPort, final int requestId, final int testId) {
    this.request = request;
    this.streamObserver = streamObserver;
    this.senderName = senderName;
    this.senderPort = senderPort;
    this.requestId = requestId;
    this.testId = testId;
    this.time = Instant.now();
  }

  public GrpcExecuteTestRequest getRequest() {
    return request;
  }

  public StreamObserver<GrpcExecuteTestResponse> getStreamObserver() {
    return streamObserver;
  }

  public Instant getTime() {
    return time;
  }

  public int getSenderPort() {
    return senderPort;
  }

  public String getSenderName() {
    return senderName;
  }

  public int getRequestId() {
    return requestId;
  }

  public int getTestId() {
    return testId;
  }
}
