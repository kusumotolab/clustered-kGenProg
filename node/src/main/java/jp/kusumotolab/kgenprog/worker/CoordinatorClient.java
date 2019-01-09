package jp.kusumotolab.kgenprog.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.ManagedChannel;
import jp.kusumotolab.kgenprog.coordinator.Coordinator;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc.CoordinatorServiceBlockingStub;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;

class CoordinatorClient {

  private static final Logger log = LoggerFactory.getLogger(CoordinatorClient.class);

  private final CoordinatorServiceBlockingStub blockingStub;

  public CoordinatorClient(final ManagedChannel managedChannel) {
    blockingStub = CoordinatorServiceGrpc.newBlockingStub(managedChannel);
  }

  public void registerWorker(final String name, final int port) {
    final GrpcRegisterWorkerRequest request = GrpcRegisterWorkerRequest.newBuilder()
        .setHost(name)
        .setPort(port)
        .build();
    log.info("registerWorker request");
    log.debug(request.toString());

    final GrpcRegisterWorkerResponse response = blockingStub.registerWorker(request);
    log.info("registerWorker response");
    log.debug(response.toString());

    if (response.getStatus() == Coordinator.STATUS_FAILED) {
      throw new RuntimeException("registerWorker failed.");
    }
  }

  public GrpcGetProjectResponse getProject(final int projectId) {
    final GrpcGetProjectRequest request = GrpcGetProjectRequest.newBuilder()
        .setProjectId(projectId)
        .build();
    log.info("getProject request");
    log.debug(request.toString());

    final GrpcGetProjectResponse response = blockingStub.getProject(request);
    log.info("getProject response");
    log.debug(response.toString());

    return response;
  }
}
