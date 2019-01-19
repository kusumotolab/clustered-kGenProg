package jp.kusumotolab.kgenprog.coordinator;

import io.grpc.stub.StreamObserver;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;

public class ExecuteTestRequest {

  private final GrpcExecuteTestRequest request;
  private final StreamObserver<GrpcExecuteTestResponse> streamObserver;

  public ExecuteTestRequest(final GrpcExecuteTestRequest request,
      final StreamObserver<GrpcExecuteTestResponse> streamObserver) {
    this.request = request;
    this.streamObserver = streamObserver;
  }

  public GrpcExecuteTestRequest getRequest() {
    return request;
  }

  public StreamObserver<GrpcExecuteTestResponse> getStreamObserver() {
    return streamObserver;
  }
}
