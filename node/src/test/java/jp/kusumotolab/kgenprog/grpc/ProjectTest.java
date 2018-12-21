package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.Test;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.variant.Base;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
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
import jp.kusumotolab.kgenprog.testutil.TestUtil;

public class ProjectTest {

  @Test
  public void testExecuteTest() {
    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    final TargetProject targetProject = TargetProjectFactory.create(rootPath);
    final GeneratedSourceCode source = TestUtil.createGeneratedSourceCode(targetProject);
    final Configuration config = new Configuration.Builder(targetProject).build();

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
    final Gene gene = new Gene(Collections.singletonList(base));

    // LocalTestExecutorでのテスト実行
    final GeneratedSourceCode modifiedCode = operation.apply(source, location);
    final TestExecutor executor = new LocalTestExecutor(config);
    final TestResults localResults = executor.exec(modifiedCode);

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

  private void assertTestResult(final TestResult remote, final TestResult local) {
    assertThat(remote.failed).isEqualTo(local.failed);
    assertThat(remote.getExecutedTargetFQNs())
        .containsExactlyElementsOf(local.getExecutedTargetFQNs());
    remote.getExecutedTargetFQNs()
        .forEach(v -> assertCoverage(remote.getCoverages(v), local.getCoverages(v)));
  }

  private void assertCoverage(final Coverage remote, final Coverage local) {
    assertThat(remote.statuses).containsExactlyElementsOf(local.statuses);
  }

}
