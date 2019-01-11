package jp.kusumotolab.kgenprog.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcGetProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterWorkerResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.Worker;
import jp.kusumotolab.kgenprog.worker.CoordinatorClient;


public class CoordinatorTest {

  @Rule
  public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final String serverName = InProcessServerBuilder.generateName();
  private final InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName)
      .directExecutor();
  private final InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName)
      .directExecutor();

  private ManagedChannel channel;
  private Coordinator coordinator;


  @Before
  public void setup() throws IOException {
    final ClusterConfiguration config = new ClusterConfiguration.Builder().build();

    coordinator = spy(new Coordinator(config));

    for (final ServerServiceDefinition service : coordinator.getServices()) {
      serverBuilder.addService(service);
    }

    grpcCleanup.register(serverBuilder.build()
        .start());
    channel = grpcCleanup.register(channelBuilder.build());
  }

  @Test
  public void testRegisterProject() {
    // レスポンスのモック作成
    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .build();

    // registerProject実行
    final KGenProgClusterBlockingStub stub = KGenProgClusterGrpc.newBlockingStub(channel);
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .build();
    final GrpcRegisterProjectResponse response1 = stub.registerProject(request);

    // requestは成功するはず
    assertThat(response1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(response1.getProjectId()).isEqualTo(response.getProjectId());
  }

  @Test
  public void testExecuteTest() {
    // レスポンスのモック作成
    final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .build();
    final Single<GrpcExecuteTestResponse> responseSingle = Single.just(response);

    final Worker worker = mock(Worker.class);
    when(worker.executeTest(any())).thenReturn(responseSingle);
    coordinator.addWorkerToLoadBalancer(worker);

    // executeTest実行
    final KGenProgClusterBlockingStub stub = KGenProgClusterGrpc.newBlockingStub(channel);
    final GrpcExecuteTestRequest executeTestRequest = GrpcExecuteTestRequest.newBuilder()
        .build();
    final GrpcExecuteTestResponse executeTestResponse = stub.executeTest(executeTestRequest);

    // レスポンス確認
    assertThat(executeTestResponse).isEqualTo(response);

    // リクエスト確認
    final ArgumentCaptor<GrpcExecuteTestRequest> captor =
        ArgumentCaptor.forClass(GrpcExecuteTestRequest.class);
    verify(worker, times(1)).executeTest(captor.capture());
    assertThat(captor.getValue()).isEqualTo(executeTestRequest);
  }

  @Test
  public void testExecuteTestError() {
    // Errorを起こしたSingleを生成
    final Worker worker = mock(Worker.class);
    final Single<GrpcExecuteTestResponse> responseSingle = Single.error(new Exception());
    when(worker.executeTest(any())).thenReturn(responseSingle);
    coordinator.addWorkerToLoadBalancer(worker);

    // executeTest実行
    final KGenProgClusterBlockingStub stub = KGenProgClusterGrpc.newBlockingStub(channel);
    final GrpcExecuteTestRequest executeTestRequest = GrpcExecuteTestRequest.newBuilder()
        .build();
    final GrpcExecuteTestResponse executeTestResponse = stub.executeTest(executeTestRequest);

    // レスポンス確認
    assertThat(executeTestResponse.getStatus()).isEqualTo(Coordinator.STATUS_FAILED);
  }

  @Test
  public void testUnregisterProject() {
    // レスポンスのモック作成
    final Worker worker = mock(Worker.class);
    final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .build();
    final Single<GrpcUnregisterProjectResponse> responseSingle = Single.just(response);

    when(worker.unregisterProject(any())).thenReturn(responseSingle);

    // unregisterProject実行
    final KGenProgClusterBlockingStub stub = KGenProgClusterGrpc.newBlockingStub(channel);
    final GrpcUnregisterProjectRequest unregisterProjectRequest =
        GrpcUnregisterProjectRequest.newBuilder()
            .build();
    final GrpcUnregisterProjectResponse unregisterProjectResponse =
        stub.unregisterProject(unregisterProjectRequest);

    // レスポンス確認
    assertThat(unregisterProjectResponse).isEqualTo(response);
  }

  @Test
  public void testRegisterWorker() {
    final CoordinatorClient coordinatorClient = new CoordinatorClient(channel);
    final GrpcRegisterWorkerResponse response = coordinatorClient.registerWorker("name", 100);
    assertThat(response.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
  }

  @Test
  public void testGetProject() {
    final KGenProgClusterBlockingStub kGenProgClusterBlockingStub = KGenProgClusterGrpc.newBlockingStub(
        channel);
    final GrpcRegisterProjectRequest registerProjectRequest = GrpcRegisterProjectRequest.newBuilder()
        .setProject(ByteString.copyFromUtf8("kGenProg"))
        .build();
    final GrpcRegisterProjectResponse registerProjectResponse = kGenProgClusterBlockingStub.registerProject(
        registerProjectRequest);
    final int projectId = registerProjectResponse.getProjectId();

    final CoordinatorClient coordinatorClient = new CoordinatorClient(channel);
    final GrpcGetProjectResponse response = coordinatorClient.getProject(projectId);

    assertThat(response.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(response.getProject()
        .toStringUtf8()).isEqualTo("kGenProg");
  }
}
