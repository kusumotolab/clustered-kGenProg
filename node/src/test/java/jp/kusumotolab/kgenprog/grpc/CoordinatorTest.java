package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import io.grpc.testing.GrpcServerRule;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.project.TargetFullyQualifiedName;
import jp.kusumotolab.kgenprog.project.build.EmptyBuildResults;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;


public class CoordinatorTest {

  @Rule
  public GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();
  private KGenProgClusterBlockingStub stub;
  private Coordinator coordinator;


  @Before
  public void setup() {
    coordinator = spy(new Coordinator(50000));

    grpcServerRule.getServiceRegistry()
        .addService(coordinator);

    stub = KGenProgClusterGrpc.newBlockingStub(grpcServerRule.getChannel());
  }


  @Test
  public void testRegisterProject() {
    final Project project = mock(Project.class);
    doReturn(project).when(coordinator)
        .createProject(anyInt(), any());

    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    final TargetProject targetProject = TargetProjectFactory.create(rootPath);
    final Configuration config = new Configuration.Builder(targetProject).build();
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .setConfiguration(Serializer.serialize(config))
        .build();

    final GrpcRegisterProjectResponse response1 = stub.registerProject(request);
    final GrpcRegisterProjectResponse response2 = stub.registerProject(request);

    // requestは成功するはず
    assertThat(response1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(response2.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);

    // projectIdは異なるはず
    assertThat(response1.getProjectId()).isNotEqualTo(response2.getProjectId());

    // Projectコンストラクタの引数の確認
    final ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(coordinator, times(2)).createProject(anyInt(), captor.capture());

    final List<Configuration> values = captor.getAllValues();
    assertThat(values).hasSize(2);

    final TargetProject targetProject1 = values.get(0)
        .getTargetProject();
    assertThat(targetProject1.rootPath).isEqualTo(targetProject.rootPath);
    assertThat(targetProject1.getClassPaths()).containsAll(targetProject.getClassPaths());
    assertThat(targetProject1.getProductSourcePaths())
        .containsExactlyElementsOf(targetProject.getProductSourcePaths());
    assertThat(targetProject1.getTestSourcePaths())
        .containsExactlyElementsOf(targetProject.getTestSourcePaths());
  }

  @Test
  public void testExecuteTest() {
    // モックの作成
    final TestResult testResult1 =
        new TestResult(new TargetFullyQualifiedName("A"), false, Collections.emptyMap());
    final TestResults testResults1 = new TestResults();
    testResults1.add(testResult1);
    testResults1.setBuildResults(EmptyBuildResults.instance);

    final TestResult testResult2 =
        new TestResult(new TargetFullyQualifiedName("B"), false, Collections.emptyMap());
    final TestResults testResults2 = new TestResults();
    testResults2.add(testResult2);
    testResults2.setBuildResults(EmptyBuildResults.instance);

    final Project project1 = mock(Project.class);
    when(project1.executeTest(any())).thenReturn(testResults1);

    final Project project2 = mock(Project.class);
    when(project2.executeTest(any())).thenReturn(testResults2);

    // 1回目の呼び出しでproject1, 2回目の呼び出しでproject2を返す
    doReturn(project1, project2).when(coordinator)
        .createProject(anyInt(), any());

    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .build();

    final int projectId1 = stub.registerProject(request)
        .getProjectId();
    final int projectId2 = stub.registerProject(request)
        .getProjectId();


    // project1.executeTestが実行ることを確認する
    final GrpcExecuteTestRequest executeTestRequest1 = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(projectId1)
        .build();
    final GrpcExecuteTestResponse executeTestResponse1 = stub.executeTest(executeTestRequest1);

    assertThat(executeTestResponse1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(executeTestResponse1.getTestResults()
        .getValueMap()).containsOnlyKeys("A");

    // project2.executeTestが実行されることを確認する
    final GrpcExecuteTestRequest executeTestRequest2 = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(projectId2)
        .build();
    final GrpcExecuteTestResponse executeTestResponse2 = stub.executeTest(executeTestRequest2);

    assertThat(executeTestResponse2.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(executeTestResponse2.getTestResults()
        .getValueMap()).containsOnlyKeys("B");
  }

  @Test
  public void testUnregisterProject() {
    final Project project = mock(Project.class);
    doReturn(project).when(coordinator)
        .createProject(anyInt(), any());

    // プロジェクトを登録する
    final GrpcRegisterProjectRequest registerRequest = GrpcRegisterProjectRequest.newBuilder()
        .build();

    final int projectId = stub.registerProject(registerRequest)
        .getProjectId();

    final GrpcUnregisterProjectRequest request = GrpcUnregisterProjectRequest.newBuilder()
        .setProjectId(projectId)
        .build();

    // 1度目は削除に成功する
    final GrpcUnregisterProjectResponse response1 = stub.unregisterProject(request);
    assertThat(response1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);

    // 2度目は失敗する
    final GrpcUnregisterProjectResponse response2 = stub.unregisterProject(request);
    assertThat(response2.getStatus()).isEqualTo(Coordinator.STATUS_FAILED);

    // unregisterが呼ばれているはず
    verify(project, times(1)).unregister();
  }
}
