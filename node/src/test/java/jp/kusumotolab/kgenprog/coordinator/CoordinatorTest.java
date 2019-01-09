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
import io.grpc.ManagedChannel;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcRegisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;
import jp.kusumotolab.kgenprog.grpc.Worker;


public class CoordinatorTest {

  @Rule
  public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final String serverName = InProcessServerBuilder.generateName();
  private final InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName)
      .directExecutor();
  private final InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName)
      .directExecutor();

  private KGenProgClusterBlockingStub stub;
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
    final ManagedChannel channel = grpcCleanup.register(channelBuilder.build());
    stub = KGenProgClusterGrpc.newBlockingStub(channel);
  }

  @Test
  public void testRegisterProject() {
    // レスポンスのモック作成
    final GrpcRegisterProjectResponse response = GrpcRegisterProjectResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .build();

//    final Single<GrpcRegisterProjectResponse> responseSingle = Single.just(response);
//
//    final Worker mockWorker = mock(Worker.class);
//    when(coordinator.createWorker(any(), any())).thenReturn(mockWorker);
//    when(mockWorker.registerProject(any(), anyInt())).thenReturn(responseSingle);

    // registerProject実行
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
    final GrpcUnregisterProjectRequest unregisterProjectRequest =
        GrpcUnregisterProjectRequest.newBuilder()
            .build();
    final GrpcUnregisterProjectResponse unregisterProjectResponse =
        stub.unregisterProject(unregisterProjectRequest);

    // レスポンス確認
    assertThat(unregisterProjectResponse).isEqualTo(response);
  }
}
