package jp.kusumotolab.kgenprog.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.coordinator.Coordinator;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc;
import jp.kusumotolab.kgenprog.grpc.CoordinatorServiceGrpc.CoordinatorServiceBlockingStub;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Project;
import jp.kusumotolab.kgenprog.project.TargetFullyQualifiedName;
import jp.kusumotolab.kgenprog.project.build.EmptyBuildResults;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;


public class LocalWorkerTest {

  @Rule
  public final GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private ManagedChannel managedChannel;
  private Path path = Paths.get("work-test-dir");

  @Before
  public void setup() {
    final String name = InProcessServerBuilder.generateName();

    managedChannel = grpcCleanupRule.register(InProcessChannelBuilder.forName(name)
        .directExecutor()
        .build());
    path = Paths.get("work-test-dir");
  }

  @Test
  public void testExecuteTest() throws IOException {

    // TestResults のの作成
    final TestResult testResult =
        new TestResult(new TargetFullyQualifiedName("A"), false, Collections.emptyMap());
    final TestResults testResults = new TestResults();
    testResults.add(testResult);
    testResults.setBuildResults(EmptyBuildResults.instance);

    // ダミーの Config
    final Configuration config = new Configuration.Builder(Paths.get(""), Collections.emptyList(),
        Collections.emptyList()).build();

    // ダミーの Project
    final Project project = mock(Project.class);
    when(project.executeTest(any())).thenReturn(testResults);
    when(project.getConfiguration()).thenReturn(config);

    // LocalWorkerの作成
    final CoordinatorClient coordinatorClient = spy(new CoordinatorClient(managedChannel));
    final LocalWorker worker = spy(new LocalWorker(path, coordinatorClient));
    doReturn(project).when(worker)
        .createProject(any(), anyInt());
    final GrpcGetProjectResponse response = GrpcGetProjectResponse.newBuilder()
        .build();
    doReturn(response).when(coordinatorClient)
        .getProject(anyInt());

    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .setProjectId(0)
        .build();

    // 二回テスト実行
    worker.executeTest(request);
    worker.executeTest(request);

    // 呼ばれているの1回のはず
    verify(worker, times(1)).createProject(any(), anyInt());
  }

  @Test
  public void testUnregisterProject() throws IOException {
    final CoordinatorServiceBlockingStub mockCoordinatorService = CoordinatorServiceGrpc.newBlockingStub(
        managedChannel);

    // Workerの作成
    final Project project = mock(Project.class);
    final CoordinatorClient coordinatorClient = spy(new CoordinatorClient(managedChannel));
    final LocalWorker worker = spy(new LocalWorker(path, coordinatorClient));
    final GrpcGetProjectResponse response = GrpcGetProjectResponse.newBuilder()
        .build();
    doReturn(project).when(worker)
        .createProject(any(), anyInt());
    doReturn(response).when(coordinatorClient)
        .getProject(anyInt());

    // プロジェクトを登録する
    final int projectId = 1;
    worker.registerProject(response, projectId);

    final GrpcUnregisterProjectRequest request = GrpcUnregisterProjectRequest.newBuilder()
        .setProjectId(projectId)
        .build();

    // 1度目は削除に成功する
    final GrpcUnregisterProjectResponse response1 = worker.unregisterProject(request)
        .blockingGet();
    assertThat(response1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);

    // 2度目は失敗する
    final GrpcUnregisterProjectResponse response2 = worker.unregisterProject(request)
        .blockingGet();
    assertThat(response2.getStatus()).isEqualTo(Coordinator.STATUS_FAILED);

    // unregisterが呼ばれているはず
    verify(project, times(1)).unregister();
  }
}
