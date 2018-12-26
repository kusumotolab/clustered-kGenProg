package jp.kusumotolab.kgenprog.client;

import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.COVERED;
import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.EMPTY;
import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.NOT_COVERED;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST01;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST02;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST03;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST04;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.grpc.Coordinator;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.Project;
import jp.kusumotolab.kgenprog.grpc.Serializer;
import jp.kusumotolab.kgenprog.project.GeneratedSourceCode;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;
import jp.kusumotolab.kgenprog.testutil.TestUtil;

public class RemoteTestExecutorTest {

  @Rule
  public final GrpcCleanupRule grpcCleanupRule = new GrpcCleanupRule();

  private String name;
  private RemoteTestExecutor remoteTestExecutor;
  private TargetProject targetProject;
  private Configuration config;


  @Before
  public void setup() {
    name = InProcessServerBuilder.generateName();

    final ManagedChannel channel = grpcCleanupRule.register(InProcessChannelBuilder.forName(name)
        .directExecutor()
        .build());

    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    targetProject = TargetProjectFactory.create(rootPath);
    config = new Configuration.Builder(targetProject).build();
    remoteTestExecutor = new RemoteTestExecutor(config, channel);
  }

  @Test
  public void testExec() throws IOException {
    setupCoordinator(config);

    final GeneratedSourceCode source = TestUtil.createGeneratedSourceCode(targetProject);
    final Variant variant = mock(Variant.class);
    when(variant.getGeneratedSourceCode()).thenReturn(source);
    when(variant.getGene()).thenReturn(new Gene(Collections.emptyList()));
    final TestResults result = remoteTestExecutor.exec(variant);

    // 実行されたテストは4個のはず
    assertThat(result.getExecutedTestFQNs()).containsExactlyInAnyOrder( //
        FOO_TEST01, FOO_TEST02, FOO_TEST03, FOO_TEST04);

    // 全テストの成否はこうなるはず
    assertThat(result.getTestResult(FOO_TEST01).failed).isFalse();
    assertThat(result.getTestResult(FOO_TEST02).failed).isFalse();
    assertThat(result.getTestResult(FOO_TEST03).failed).isTrue();
    assertThat(result.getTestResult(FOO_TEST04).failed).isFalse();

    // よってテストの成功率はこうなる
    assertThat(result.getSuccessRate()).isEqualTo(1.0 * 3 / 4);

    final TestResult fooTest01result = result.getTestResult(FOO_TEST01);
    final TestResult fooTest04result = result.getTestResult(FOO_TEST04);

    // FooTest.test01 実行によるFooのカバレッジはこうなるはず
    assertThat(fooTest01result.getCoverages(FOO).statuses).containsExactly(EMPTY, COVERED, EMPTY,
        COVERED, COVERED, EMPTY, EMPTY, NOT_COVERED, EMPTY, COVERED);

    // FooTest.test04 実行によるFooのバレッジはこうなるはず
    assertThat(fooTest04result.getCoverages(FOO).statuses).containsExactly(EMPTY, COVERED, EMPTY,
        COVERED, NOT_COVERED, EMPTY, EMPTY, COVERED, EMPTY, COVERED);
  }

  private void setupCoordinator(final Configuration config) throws IOException {
    final Coordinator coordinator = mock(Coordinator.class);

    final Server server = InProcessServerBuilder.forName(name)
        .directExecutor()
        .addService(coordinator)
        .build()
        .start();
    grpcCleanupRule.register(server);

    final Project project = new Project(0, config);

    doAnswer(invocation -> {
      final GrpcExecuteTestRequest request = invocation.getArgument(0);
      final StreamObserver<GrpcExecuteTestResponse> responseObserver = invocation.getArgument(1);
      final Gene gene = Serializer.deserialize(targetProject.rootPath, request.getGene());
      final TestResults results = project.executeTest(gene);
      final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
          .setStatus(Coordinator.STATUS_SUCCESS)
          .setTestResults(Serializer.serialize(results))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
      return null;
    }).when(coordinator)
        .executeTest(any(), any());
  }
}

