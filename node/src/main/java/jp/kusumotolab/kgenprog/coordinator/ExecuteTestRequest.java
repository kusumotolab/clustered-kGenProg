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

  public ExecuteTestRequest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> streamObserver, final String senderName,
      final int senderPort) {
    this.request = request;
    this.streamObserver = streamObserver;
    this.senderName = senderName;
    this.senderPort = senderPort;
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
}
