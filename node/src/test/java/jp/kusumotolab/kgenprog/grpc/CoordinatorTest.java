package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.reactivex.Single;
import jp.kusumotolab.kgenprog.grpc.KGenProgClusterGrpc.KGenProgClusterBlockingStub;


public class CoordinatorTest {

  @Rule
  public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private final String serverName = InProcessServerBuilder.generateName();
  private final InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName)
      .directExecutor();
  private final InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName)
      .directExecutor();

  private KGenProgClusterBlockingStub stub;
  private Worker worker;
  private Coordinator coordinator;


  @Before
  public void setup() throws IOException {
    worker = mock(Worker.class);
    coordinator = new Coordinator(50000, worker);
    grpcCleanup.register(serverBuilder.addService(coordinator)
        .build()
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

    final Single<GrpcRegisterProjectResponse> responseSingle = Single.just(response);

    when(worker.registerProject(any(), anyInt())).thenReturn(responseSingle);

    // registerProject実行
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .build();
    final GrpcRegisterProjectResponse response1 = stub.registerProject(request);

    // requestは成功するはず
    assertThat(response1.getStatus()).isEqualTo(Coordinator.STATUS_SUCCESS);
    assertThat(response1.getProjectId()).isEqualTo(response.getProjectId());

    // もう一度呼び出し
    stub.registerProject(request);

    // LocalWorkerへの引数の確認
    final ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    verify(worker, times(2)).registerProject(any(), captor.capture());

    // projectIdは異なるはず
    final List<Integer> values = captor.getAllValues();
    assertThat(values).hasSize(2);
    assertThat(values.get(0)).isNotEqualTo(values.get(1));
  }

  @Test
  public void testRegisterProjectError() {
    // Errorを起こしたSingleを生成
    final Single<GrpcRegisterProjectResponse> responseSingle = Single.error(new Exception());
    when(worker.registerProject(any(), anyInt())).thenReturn(responseSingle);

    // registerProject実行
    final GrpcRegisterProjectRequest request = GrpcRegisterProjectRequest.newBuilder()
        .build();
    final GrpcRegisterProjectResponse response = stub.registerProject(request);

    // requestが失敗するはず
    assertThat(response.getStatus()).isEqualTo(Coordinator.STATUS_FAILED);
  }

  @Test
  public void testExecuteTest() {
    // レスポンスのモック作成
    final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
        .setStatus(Coordinator.STATUS_SUCCESS)
        .build();
    final Single<GrpcExecuteTestResponse> responseSingle = Single.just(response);

    when(worker.executeTest(any())).thenReturn(responseSingle);

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
    final Single<GrpcExecuteTestResponse> responseSingle = Single.error(new Exception());
    when(worker.executeTest(any())).thenReturn(responseSingle);

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

    // リクエスト確認
    final ArgumentCaptor<GrpcUnregisterProjectRequest> captor =
        ArgumentCaptor.forClass(GrpcUnregisterProjectRequest.class);
    verify(worker, times(1)).unregisterProject(captor.capture());
    assertThat(captor.getValue()).isEqualTo(unregisterProjectRequest);
  }

  @Test
  public void testUnregisterProjectError() {
    // Errorを起こしたSingleを生成
    final Single<GrpcUnregisterProjectResponse> responseSingle = Single.error(new Exception());
    when(worker.unregisterProject(any())).thenReturn(responseSingle);

    // unregisterProject実行
    final GrpcUnregisterProjectRequest unregisterProjectRequest =
        GrpcUnregisterProjectRequest.newBuilder()
            .build();
    final GrpcUnregisterProjectResponse unregisterProjectResponse =
        stub.unregisterProject(unregisterProjectRequest);

    // レスポンス確認
    assertThat(unregisterProjectResponse.getStatus()).isEqualTo(Coordinator.STATUS_FAILED);
  }
}
