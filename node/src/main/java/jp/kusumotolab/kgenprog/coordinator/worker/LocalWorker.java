package jp.kusumotolab.kgenprog.coordinator.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.grpc.ManagedChannel;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.coordinator.Coordinator;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc.CoordinatorServiceBlockingStub;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Project;
import jp.kusumotolab.kgenprog.grpc.Serializer;
import jp.kusumotolab.kgenprog.grpc.Worker;
import jp.kusumotolab.kgenprog.project.test.TestResults;

/**
 * ローカルでテストを実行するワーカー
 *
 * @author Ryo Arima
 */
public class LocalWorker implements Worker {

  private final ConcurrentMap<Integer, Project> projectMap;
  private final Path workdir;
  private final CoordinatorServiceBlockingStub blockingStub;

  public LocalWorker(final Path workdir, final CoordinatorServiceBlockingStub blockingStub) {
    this.workdir = workdir;
    projectMap = new ConcurrentHashMap<>();
    this.blockingStub = blockingStub;
  }

  @Override
  public Single<GrpcExecuteTestResponse> executeTest(final GrpcExecuteTestRequest request) {
    final Single<GrpcExecuteTestResponse> responseSingle;
    Project project = projectMap.get(request.getProjectId());
    if (project == null) {
      // プロジェクトが見つからなかった場合、プロジェクトをCoordinatorに問い合わせる
      final GrpcGetProjectRequest projectRequest = GrpcGetProjectRequest.newBuilder()
          .setProjectId(request.getProjectId())
          .build();
      final GrpcGetProjectResponse response = getProject(projectRequest);
      project = registerProject(response, request.getProjectId());
    }
    final Path rootPath = project.getConfiguration()
        .getTargetProject().rootPath;
    final Gene gene = Serializer.deserialize(rootPath, request.getGene());
    final Single<Gene> geneSingle = Single.just(gene);
    final Single<TestResults> resultsSingle = geneSingle.map(project::executeTest);
    responseSingle = resultsSingle.map(results -> GrpcExecuteTestResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .setTestResults(Serializer.serialize(results))
        .build());

    return responseSingle;
  }

  Project registerProject(final GrpcGetProjectResponse request, final int projectId) {
    try {
      final Project project = createProject(request, projectId);
      projectMap.put(projectId, project);
      return project;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Single<GrpcUnregisterProjectResponse> unregisterProject(
      final GrpcUnregisterProjectRequest request) {
    try {
      final GrpcUnregisterProjectResponse response;
      final Project project = projectMap.remove(request.getProjectId());
      if (project == null) {
        // プロジェクトが見つからなかった場合、実行失敗メッセージを返す
        response = GrpcUnregisterProjectResponse.newBuilder()
            .setStatus(Coordinator.STATUS_FAILED)
            .build();

      } else {
        project.unregister();
        response = GrpcUnregisterProjectResponse.newBuilder()
            .setStatus(Coordinator.STATUS_SUCCESS)
            .build();
      }

      return Single.just(response);
    } catch (final Exception e) {
      return Single.error(e);
    }
  }

  /**
   * 新たなプロジェクトを生成する
   *
   * テストの際にモックとして差し替えることを想定している
   *
   * @param response プロジェクトID
   * @param projectId プロジェクトのConfiguration
   * @return 生成されたプロジェクト
   * @throws IOException
   */
  protected Project createProject(final GrpcGetProjectResponse response, final int projectId)
      throws IOException {
    return new Project(workdir, response, projectId);
  }

  protected GrpcGetProjectResponse getProject(final GrpcGetProjectRequest request) {
    return blockingStub.getProject(request);
  }
}
