package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.project.test.TestResults;

/**
 * ローカルでテストを実行するワーカー
 * 
 * @author Ryo Arima
 *
 */
public class LocalWorker implements Worker {

  private final ConcurrentMap<Integer, Project> projectMap;
  private final Path workdir = null;

  public LocalWorker() {
    projectMap = new ConcurrentHashMap<>();
  }

  @Override
  public Single<GrpcRegisterProjectResponse> registerProject(
      final GrpcRegisterProjectRequest request, final int projectId) {
    try {
      final Project project = createProject(request, projectId);
      projectMap.put(projectId, project);

      final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
          .setProjectId(projectId)
          .setStatus(Coordinator.STATUS_SUCCESS)
          .build();

      return Single.just(response);

    } catch (Exception e) {
      return Single.error(e);
    }
  }

  @Override
  public Single<GrpcExecuteTestResponse> executeTest(final GrpcExecuteTestRequest request) {
    final Single<GrpcExecuteTestResponse> responseSingle;
    final Project project = projectMap.get(request.getProjectId());
    if (project == null) {
      // プロジェクトが見つからなかった場合、実行失敗メッセージを返す
      final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(Coordinator.STATUS_FAILED)
          .build();
      responseSingle = Single.just(response);

    } else {
      final Gene gene = Serializer.deserialize(request.getGene());
      final Single<Gene> geneSingle = Single.just(gene);
      final Single<TestResults> resultsSingle = geneSingle.map(project::executeTest);
      responseSingle = resultsSingle.map(results -> GrpcExecuteTestResponse.newBuilder()
          .setStatus(Coordinator.STATUS_SUCCESS)
          .setTestResults(Serializer.serialize(results))
          .build());
    }

    return responseSingle;
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
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  /**
   * 新たなプロジェクトを生成する
   * 
   * テストの際にモックとして差し替えることを想定している
   * 
   * @param request プロジェクトID
   * @param projectId プロジェクトのConfiguration
   * @return 生成されたプロジェクト
   * @throws IOException
   */
  protected Project createProject(final GrpcRegisterProjectRequest request, final int projectId)
      throws IOException {
    return new Project(workdir, request, projectId);
  }


}
