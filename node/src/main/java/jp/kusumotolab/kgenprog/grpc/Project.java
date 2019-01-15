package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.Strategies;
import jp.kusumotolab.kgenprog.fl.FaultLocalization;
import jp.kusumotolab.kgenprog.fl.Ochiai;
import jp.kusumotolab.kgenprog.ga.codegeneration.DefaultSourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.codegeneration.SourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.selection.GenerationalVariantSelection;
import jp.kusumotolab.kgenprog.ga.selection.VariantSelection;
import jp.kusumotolab.kgenprog.ga.validation.DefaultCodeValidation;
import jp.kusumotolab.kgenprog.ga.validation.SourceCodeValidation;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.ga.variant.VariantStore;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.test.LocalTestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class Project {

  private final Path projectDir;
  private final Configuration config;
  private final int projectId;
  private final Strategies strategies;
  private final VariantStore variantStore;

  /**
   * ローカルに存在するプロジェクトに対するProjectを生成
   * 
   * @param projectId プロジェクトID
   * @param config Configuration
   */
  public Project(final int projectId, final Configuration config) {
    this.config = config;
    this.projectId = projectId;
    this.projectDir = null;

    this.strategies = createStrategies(this.config);
    this.variantStore = new VariantStore(this.config, strategies);
  }

  /**
   * プロジェクト本体がZIP圧縮されたプロジェクトに対するProjectを生成
   * 
   * @param workdir 作業ディレクトリ
   * @param response gRPCリクエスト
   * @param projectId プロジェクトID
   * @throws IOException
   */
  public Project(final Path workdir, final GrpcGetProjectResponse response, final int projectId)
      throws IOException {

    this.projectId = projectId;
    this.projectDir = workdir.resolve(Integer.toString(projectId));
    this.config = unzipProject(projectDir, response);
    this.strategies = createStrategies(this.config);
    this.variantStore = new VariantStore(this.config, strategies);
  }

  public TestResults executeTest(final Gene gene) {
    final Variant variant = variantStore.createVariant(gene, EmptyHistoricalElement.shared);
    return variant.getTestResults();
  }

  public void unregister() throws IOException {
    if (projectDir == null) {
      return;
    }

    // プロジェクトディレクトリ以下のファイルをすべて削除
    Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
          throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
          throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public int getProjectId() {
    return projectId;
  }

  public Configuration getConfiguration() {
    return config;
  }

  private Configuration unzipProject(final Path projectDir,
      final GrpcGetProjectResponse response) throws IOException {

    final Configuration config = Serializer.deserialize(response.getConfiguration());
    Files.createDirectory(projectDir);
    final TargetProject project = ProjectUnzipper.unzipProject(projectDir,
        config.getTargetProject(), response.getProject()::newInput);
    final GrpcConfiguration updateConfig = Serializer.updateConfiguration(response.getConfiguration()
        .toBuilder(), project)
        .build();

    return Serializer.deserialize(updateConfig);
  }

  private Strategies createStrategies(final Configuration configuration) {
    final FaultLocalization faultLocaliztion = new Ochiai();
    final JDTASTConstruction astConstruction = new JDTASTConstruction();
    final SourceCodeGeneration sourceCodeGeneration = new DefaultSourceCodeGeneration();
    final SourceCodeValidation sourceCodeValidation = new DefaultCodeValidation();
    final TestExecutor testExecutor = new LocalTestExecutor(configuration);
    final VariantSelection variantSelection =
        new GenerationalVariantSelection(configuration.getHeadcount());
    return new Strategies(faultLocaliztion, astConstruction, sourceCodeGeneration,
        sourceCodeValidation, testExecutor, variantSelection);
  }

}
