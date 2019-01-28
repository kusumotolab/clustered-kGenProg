package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.google.protobuf.ByteString;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.variant.Base;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.project.GeneratedSourceCode;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.project.jdt.ASTStream;
import jp.kusumotolab.kgenprog.project.jdt.DeleteOperation;
import jp.kusumotolab.kgenprog.project.jdt.GeneratedJDTAST;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTLocation;
import jp.kusumotolab.kgenprog.project.test.Coverage;
import jp.kusumotolab.kgenprog.project.test.LocalTestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;
import jp.kusumotolab.kgenprog.testutil.CoverageUtil;
import jp.kusumotolab.kgenprog.testutil.TestUtil;

public class ProjectTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Configuration config;
  private TestResults localResults;
  private Gene gene;

  @Before
  public void setup() {
    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    final TargetProject targetProject = TargetProjectFactory.create(rootPath);
    final GeneratedSourceCode source = TestUtil.createGeneratedSourceCode(targetProject);
    config = new Configuration.Builder(targetProject).build();

    final GeneratedJDTAST<ProductSourcePath> generatedAST =
        (GeneratedJDTAST<ProductSourcePath>) source.getProductAsts()
            .get(0);

    final ASTNode node = ASTStream.stream(generatedAST.getRoot())
        .filter(Statement.class::isInstance)
        .skip(3)
        .findFirst()
        .get();

    final DeleteOperation operation = new DeleteOperation();
    final JDTASTLocation location =
        new JDTASTLocation(generatedAST.getSourcePath(), node, generatedAST);
    final Base base = new Base(location, operation);
    gene = new Gene(Collections.singletonList(base));

    // 比較対象としてLocalTestExecutorでのテスト結果を用意
    final GeneratedSourceCode modifiedCode = operation.apply(source, location);
    final Variant variant = mock(Variant.class);
    when(variant.getGeneratedSourceCode()).thenReturn(modifiedCode);
    final TestExecutor executor = new LocalTestExecutor(config);
    localResults = executor.exec(variant);
  }

  @Test
  public void testExecuteTest() {
    // Projectでのテスト実行
    final Project project = new Project(0, config);
    final TestResults remoteResults = project.executeTest(gene);

    // TestResultsが等しいか確認する
    assertThat(remoteResults.getExecutedTestFQNs())
        .containsExactlyElementsOf(localResults.getExecutedTestFQNs());

    remoteResults.getExecutedTestFQNs()
        .forEach(
            v -> assertTestResult(remoteResults.getTestResult(v), localResults.getTestResult(v)));
  }

  @Test
  public void testExecuteTestZipProject() throws IOException {
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    final TargetProject targetProject =
        ProjectZipper.zipProject(config.getTargetProject(), () -> stream);

    final GrpcConfiguration grpcConfig = Serializer.updateConfiguration(Serializer.serialize(config)
        .toBuilder(), targetProject)
        .build();

    final GrpcGetProjectResponse response = GrpcGetProjectResponse.newBuilder()
        .setConfiguration(grpcConfig)
        .setProject(ByteString.copyFrom(stream.toByteArray()))
        .build();

    final Path workdir = tempFolder.newFolder()
        .toPath();

    // Projectでのテスト実行
    final Project project = new Project(workdir, response, 0);
    final TestResults remoteResults = project.executeTest(gene);

    // TestResultsが等しいか確認する
    assertThat(remoteResults.getExecutedTestFQNs())
        .containsExactlyElementsOf(localResults.getExecutedTestFQNs());

    remoteResults.getExecutedTestFQNs()
        .forEach(
            v -> assertTestResult(remoteResults.getTestResult(v), localResults.getTestResult(v)));

    project.unregister();

    // ファイルが削除されていることを確認
    try (final Stream<Path> fileStream = Files.list(workdir)) {
      assertThat(fileStream.count()).isEqualTo(0);
    }
  }

  private void assertTestResult(final TestResult remote, final TestResult local) {
    assertThat(remote.failed).isEqualTo(local.failed);
    assertThat(remote.getExecutedTargetFQNs())
        .containsExactlyElementsOf(local.getExecutedTargetFQNs());
    remote.getExecutedTargetFQNs()
        .forEach(v -> assertCoverage(remote.getCoverages(v), local.getCoverages(v)));
  }

  private void assertCoverage(final Coverage remote, final Coverage local) {
    assertThat(CoverageUtil.extractStatuses(remote)).containsExactlyElementsOf(
        CoverageUtil.extractStatuses(local));
  }
}
