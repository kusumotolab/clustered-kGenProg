package jp.kusumotolab.kgenprog.grpc;

import static jp.kusumotolab.kgenprog.project.jdt.ASTNodeAssert.assertThat;
import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.COVERED;
import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.EMPTY;
import static jp.kusumotolab.kgenprog.project.test.Coverage.Status.NOT_COVERED;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST01;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST02;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST03;
import static jp.kusumotolab.kgenprog.testutil.ExampleAlias.Fqn.FOO_TEST04;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.Test;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.Base;
import jp.kusumotolab.kgenprog.ga.Gene;
import jp.kusumotolab.kgenprog.grpc.GrpcCoverage.Status;
import jp.kusumotolab.kgenprog.project.GeneratedSourceCode;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.project.jdt.ASTStream;
import jp.kusumotolab.kgenprog.project.jdt.DeleteOperation;
import jp.kusumotolab.kgenprog.project.jdt.GeneratedJDTAST;
import jp.kusumotolab.kgenprog.project.jdt.InsertOperation;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTLocation;
import jp.kusumotolab.kgenprog.project.jdt.JDTOperation;
import jp.kusumotolab.kgenprog.project.jdt.ReplaceOperation;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.LocalTestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;
import jp.kusumotolab.kgenprog.testutil.ExampleAlias.Src;
import jp.kusumotolab.kgenprog.testutil.TestUtil;

public class SerializerTest {

  @Test
  public void testSerializeConfiguration() {
    final Path rootDir = Paths.get("../main/example/BuildSuccess02");
    final List<Path> productPaths =
        Arrays.asList(rootDir.resolve(Src.FOO), rootDir.resolve(Src.BAR));
    final List<Path> testPaths =
        Arrays.asList(rootDir.resolve(Src.FOO_TEST), rootDir.resolve(Src.BAR_TEST));
    final List<Path> classPaths =
        Arrays.asList(rootDir.resolve("src/main/resources/junit4/junit-4.12.jar"));
    final Configuration config =
        new Configuration.Builder(rootDir, productPaths, testPaths).addClassPaths(classPaths)
            .addExecutionTest("example.FooTest")
            .addExecutionTest("example.BarTest")
            .setTestTimeLimitSeconds(10)
            .build();

    final GrpcConfiguration grpcConfig = Serializer.serialize(config);

    assertThat(grpcConfig.getRootDir()).isEqualTo(rootDir.toString());
    assertThat(grpcConfig.getProductPathsList()).containsExactly(rootDir.resolve(Src.FOO)
        .toString(),
        rootDir.resolve(Src.BAR)
            .toString());
    assertThat(grpcConfig.getTestPathsList()).containsExactly(rootDir.resolve(Src.FOO_TEST)
        .toString(),
        rootDir.resolve(Src.BAR_TEST)
            .toString());
    assertThat(grpcConfig.getClassPathsList())
        .contains(rootDir.resolve("src/main/resources/junit4/junit-4.12.jar")
            .toString());
    assertThat(grpcConfig.getExecutionTestsList()).containsExactly("example.FooTest",
        "example.BarTest");
    assertThat(grpcConfig.getTestTimeLimit()).isEqualTo(10);

    // デシリアライズの実行
    final Configuration deserialized = Serializer.deserialize(grpcConfig);
    final TargetProject targetProject = deserialized.getTargetProject();
    assertThat(targetProject.rootPath).isEqualTo(rootDir);
    assertThat(targetProject.getProductSourcePaths()).extracting(p -> p.path)
        .containsExactlyElementsOf(productPaths);
    assertThat(targetProject.getTestSourcePaths()).extracting(p -> p.path)
        .containsExactlyElementsOf(testPaths);
    assertThat(deserialized.getExecutedTests()).containsExactly("example.FooTest",
        "example.BarTest");
    assertThat(deserialized.getTestTimeLimitSeconds()).isEqualTo(10);
  }

  @Test
  public void testSerializeGene() {
    final String fileName = "A.java";
    final String source = new StringBuilder().append("")
        .append("class A {\n")
        .append("  public void a() {\n")
        .append("    int n = 0;\n")
        .append("    if (n == 1) {\n")
        .append("      System.out.println(n);\n")
        .append("    }\n")
        .append("  }\n")
        .append("  public int b(int n) {\n")
        .append("    if (n < 0) { return -n; }\n")
        .append("    return n;\n")
        .append("  }\n")
        .append("}\n")
        .toString();
    final ProductSourcePath productSourcePath = new ProductSourcePath(Paths.get(fileName));
    final JDTASTConstruction constructor = new JDTASTConstruction();
    final GeneratedJDTAST<ProductSourcePath> ast =
        constructor.constructAST(productSourcePath, source);

    final List<Statement> statements = ASTStream.stream(ast.getRoot())
        .filter(Statement.class::isInstance)
        .map(Statement.class::cast)
        .collect(Collectors.toList());

    // Deleteを行うBase作成
    final JDTASTLocation location1 = new JDTASTLocation(productSourcePath, statements.get(1), ast);
    final JDTOperation operation1 = new DeleteOperation();
    final Base base1 = new Base(location1, operation1);

    // Insertを行うBase作成
    final JDTASTLocation location2 = new JDTASTLocation(productSourcePath, statements.get(2), ast);
    final JDTOperation operation2 = new InsertOperation(statements.get(6));
    final Base base2 = new Base(location2, operation2);

    // Replaceを行うBase作成
    final JDTASTLocation location3 = new JDTASTLocation(productSourcePath, statements.get(4), ast);
    final JDTOperation operation3 = new ReplaceOperation(statements.get(8));
    final Base base3 = new Base(location3, operation3);

    final Gene gene = new Gene(Arrays.asList(base1, base2, base3));

    // Grpcオブジェクトへシリアライズ
    final GrpcGene grpcGene = Serializer.serialize(gene);

    // assertions

    // baseの数の確認
    assertThat(grpcGene.getBaseCount()).isEqualTo(3);

    // Delete
    final GrpcBase grpcBase1 = grpcGene.getBase(0);
    assertThat(grpcBase1.getLocation()
        .getSourcePath()).isEqualTo("A.java");
    assertTreePathElement(grpcBase1.getLocation()
        .getLocationList(), new String[] {"types", "bodyDeclarations", "body", "statements"},
        new int[] {0, 0, 0, 0});
    assertThat(grpcBase1.getOperation()
        .getType()).isEqualTo(GrpcOperation.Type.DELETE);

    // Insert
    final GrpcBase grpcBase2 = grpcGene.getBase(1);
    assertThat(grpcBase2.getLocation()
        .getSourcePath()).isEqualTo("A.java");
    assertTreePathElement(grpcBase2.getLocation()
        .getLocationList(), new String[] {"types", "bodyDeclarations", "body", "statements"},
        new int[] {0, 0, 0, 1});
    assertThat(grpcBase2.getOperation()
        .getType()).isEqualTo(GrpcOperation.Type.INSERT);
    assertThat(grpcBase2.getOperation()
        .getStatement()).isEqualTo("if (n < 0) {\n  return -n;\n}\n");

    // Replace
    final GrpcBase grpcBase3 = grpcGene.getBase(2);
    assertThat(grpcBase3.getLocation()
        .getSourcePath()).isEqualTo("A.java");
    assertTreePathElement(grpcBase3.getLocation()
        .getLocationList(),
        new String[] {"types", "bodyDeclarations", "body", "statements", "thenStatement",
            "statements"},
        new int[] {0, 0, 0, 1, -1, 0});
    assertThat(grpcBase3.getOperation()
        .getType()).isEqualTo(GrpcOperation.Type.REPLACE);
    assertThat(grpcBase3.getOperation()
        .getStatement()).isEqualTo("return -n;\n");

    // デシリアライズの実行
    final Gene deserialized = Serializer.deserialize(grpcGene);
    assertThat(deserialized.getBases()).hasSize(3);

    // delete
    final Base deserializedBase1 = deserialized.getBases()
        .get(0);
    assertThat(deserializedBase1.getTargetLocation()).isInstanceOf(RemoteJDTASTLocation.class);
    final RemoteJDTASTLocation rlocation1 =
        (RemoteJDTASTLocation) deserializedBase1.getTargetLocation();
    assertThat(rlocation1.getSourcePath().path).isEqualTo(location1.getSourcePath().path);
    assertTreePathElement(rlocation1.getPathToLocation(),
        new String[] {"types", "bodyDeclarations", "body", "statements"}, new int[] {0, 0, 0, 0});

    assertThat(deserializedBase1.getOperation()).isInstanceOf(DeleteOperation.class);

    // insert
    final Base deserializedBase2 = deserialized.getBases()
        .get(1);
    assertThat(deserializedBase2.getTargetLocation()).isInstanceOf(RemoteJDTASTLocation.class);
    final RemoteJDTASTLocation rlocation2 =
        (RemoteJDTASTLocation) deserializedBase2.getTargetLocation();
    assertThat(rlocation2.getSourcePath().path).isEqualTo(location2.getSourcePath().path);
    assertTreePathElement(rlocation2.getPathToLocation(),
        new String[] {"types", "bodyDeclarations", "body", "statements"}, new int[] {0, 0, 0, 1});

    assertThat(deserializedBase2.getOperation()).isInstanceOf(InsertOperation.class);
    assertThat(((InsertOperation) deserializedBase2.getOperation()).getNode())
        .isSameSourceCodeAs(statements.get(6));

    // Replace
    final Base deserializedBase3 = deserialized.getBases()
        .get(2);
    assertThat(deserializedBase3.getTargetLocation()).isInstanceOf(RemoteJDTASTLocation.class);
    final RemoteJDTASTLocation rlocation3 =
        (RemoteJDTASTLocation) deserializedBase3.getTargetLocation();
    assertThat(rlocation3.getSourcePath().path).isEqualTo(location3.getSourcePath().path);
    assertTreePathElement(rlocation3.getPathToLocation(), new String[] {"types", "bodyDeclarations",
        "body", "statements", "thenStatement", "statements"}, new int[] {0, 0, 0, 1, -1, 0});

    assertThat(deserializedBase3.getOperation()).isInstanceOf(ReplaceOperation.class);
    assertThat(((ReplaceOperation) deserializedBase3.getOperation()).getNode())
        .isSameSourceCodeAs(statements.get(8));
  }

  private void assertTreePathElement(final List<GrpcTreePathElement> target, final String[] id,
      final int[] idx) {
    assertThat(id.length).isEqualTo(idx.length);
    assertThat(target.size()).isEqualTo(id.length);
    for (int i = 0; i < target.size(); i++) {
      final GrpcTreePathElement element = target.get(i);
      assertThat(element.getPropertyId()).isEqualTo(id[i]);
      if (idx[i] != -1) {
        assertThat(element.getIndex()).isEqualTo(idx[i]);
      }
    }
  }

  @Test
  public void testSerializeTestResults() {
    // Testの実行
    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    final TargetProject targetProject = TargetProjectFactory.create(rootPath);
    final GeneratedSourceCode source = TestUtil.createGeneratedSourceCode(targetProject);

    final Configuration config = new Configuration.Builder(targetProject).build();
    final TestExecutor executor = new LocalTestExecutor(config);
    final TestResults testResults = executor.exec(null, source);

    // シリアライズ実行
    final GrpcTestResults grpcTestResults = Serializer.serialize(testResults);

    // テスト成功を確認
    assertThat(grpcTestResults.getEmpty()).isFalse();

    final Map<String, GrpcTestResult> valueMap = grpcTestResults.getValueMap();
    assertThat(valueMap).containsOnlyKeys("example.FooTest.test01", "example.FooTest.test02",
        "example.FooTest.test03", "example.FooTest.test04");

    // test01
    final GrpcTestResult testResult1 = valueMap.get("example.FooTest.test01");
    assertThat(testResult1.getExecutedTestFQN()).isEqualTo("example.FooTest.test01");
    assertThat(testResult1.getFailed()).isFalse();
    assertThat(testResult1.getCoverageMap()).containsOnlyKeys("example.Foo");
    final GrpcCoverage grpcCoverage1 = testResult1.getCoverageMap()
        .get("example.Foo");
    assertThat(grpcCoverage1.getExecutedTargetFQN()).isEqualTo("example.Foo");
    assertThat(grpcCoverage1.getStatusList()).containsExactly(Status.EMPTY, Status.COVERED,
        Status.EMPTY, Status.COVERED, Status.COVERED, Status.EMPTY, Status.EMPTY,
        Status.NOT_COVERED, Status.EMPTY, Status.COVERED);

    // test03
    final GrpcTestResult testResult3 = valueMap.get("example.FooTest.test03");
    assertThat(testResult3.getExecutedTestFQN()).isEqualTo("example.FooTest.test03");
    assertThat(testResult3.getFailed()).isTrue();
    assertThat(testResult3.getCoverageMap()).containsOnlyKeys("example.Foo");
    final GrpcCoverage grpcCoverage3 = testResult3.getCoverageMap()
        .get("example.Foo");
    assertThat(grpcCoverage3.getExecutedTargetFQN()).isEqualTo("example.Foo");
    assertThat(grpcCoverage3.getStatusList()).containsExactly(Status.EMPTY, Status.COVERED,
        Status.EMPTY, Status.COVERED, Status.NOT_COVERED, Status.EMPTY, Status.EMPTY,
        Status.COVERED, Status.EMPTY, Status.COVERED);

    // BuildResults
    final GrpcBuildResults grpcBuildResults = grpcTestResults.getBuildResults();
    final Path foo = Paths.get("../main/example/BuildSuccess01/src/example/Foo.java");
    final Path fooTest = Paths.get("../main/example/BuildSuccess01/src/example/FooTest.java");
    assertThat(grpcBuildResults.getSourcePathToFQNMap()).containsOnlyKeys(foo.toString(),
        fooTest.toString());
    final GrpcFullyQualifiedNames grpcFullyQualifiedNames = grpcBuildResults.getSourcePathToFQNMap()
        .get(foo.toString());
    assertThat(grpcFullyQualifiedNames.getNameList()).containsExactly("example.Foo");


    // デシリアライズ実行
    final TestResults deserialized = Serializer.deserialize(grpcTestResults);
    assertThat(deserialized).isNotSameAs(EmptyTestResults.instance);

    assertThat(deserialized.getExecutedTestFQNs()).containsExactlyInAnyOrder( //
        FOO_TEST01, FOO_TEST02, FOO_TEST03, FOO_TEST04);

    assertThat(deserialized.getTestResult(FOO_TEST01).failed).isFalse();
    assertThat(deserialized.getTestResult(FOO_TEST02).failed).isFalse();
    assertThat(deserialized.getTestResult(FOO_TEST03).failed).isTrue();
    assertThat(deserialized.getTestResult(FOO_TEST04).failed).isFalse();

    assertThat(deserialized.getSuccessRate()).isEqualTo(1.0 * 3 / 4);

    final TestResult fooTest01result = deserialized.getTestResult(FOO_TEST01);
    final TestResult fooTest04result = deserialized.getTestResult(FOO_TEST04);

    assertThat(fooTest01result.getCoverages(FOO).statuses).containsExactly(EMPTY, COVERED, EMPTY,
        COVERED, COVERED, EMPTY, EMPTY, NOT_COVERED, EMPTY, COVERED);

    assertThat(fooTest04result.getCoverages(FOO).statuses).containsExactly(EMPTY, COVERED, EMPTY,
        COVERED, NOT_COVERED, EMPTY, EMPTY, COVERED, EMPTY, COVERED);

  }
}
