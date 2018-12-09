package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.Test;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.Base;
import jp.kusumotolab.kgenprog.ga.Gene;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.jdt.ASTStream;
import jp.kusumotolab.kgenprog.project.jdt.DeleteOperation;
import jp.kusumotolab.kgenprog.project.jdt.GeneratedJDTAST;
import jp.kusumotolab.kgenprog.project.jdt.InsertOperation;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTLocation;
import jp.kusumotolab.kgenprog.project.jdt.JDTOperation;
import jp.kusumotolab.kgenprog.project.jdt.ReplaceOperation;
import jp.kusumotolab.kgenprog.testutil.ExampleAlias.Src;

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

}
