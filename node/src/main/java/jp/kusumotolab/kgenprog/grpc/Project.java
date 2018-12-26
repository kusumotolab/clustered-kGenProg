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
   * @param request gRPCリクエスト
   * @param projectId プロジェクトID
   * @throws IOException
   */
  public Project(final Path workdir, final GrpcRegisterProjectRequest request, final int projectId)
      throws IOException {

    this.projectId = projectId;
    this.projectDir = workdir.resolve(Integer.toString(projectId));
    this.config = unzipProject(projectDir, request);
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
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public int getProjectId() {
    return projectId;
  }

  private Configuration unzipProject(final Path projectDir,
      final GrpcRegisterProjectRequest request) throws IOException {

    final Configuration config = Serializer.deserialize(request.getConfiguration());
    Files.createDirectory(projectDir);
    final TargetProject project = ProjectUnzipper.unzipProject(projectDir, config.getTargetProject(),
        request.getProject()::newInput);
    final GrpcConfiguration updateConfig = updateConfig(request.getConfiguration(), project);

    return Serializer.deserialize(updateConfig);
  }

  /**
   * ConfigurationのTargetProjectを更新する
   * 
   * @param origin もとになるConfiguration
   * @param project 更新内容の含まれるTargetProject
   * @return 更新されたConfiguration
   */
  private GrpcConfiguration updateConfig(final GrpcConfiguration origin,
      final TargetProject project) {
    final Iterable<String> productPaths = () -> project.getProductSourcePaths()
        .stream()
        .map(v -> v.path.toString())
        .iterator();

    final Iterable<String> testPaths = () -> project.getTestSourcePaths()
        .stream()
        .map(v -> v.path.toString())
        .iterator();

    final Iterable<String> classPaths = () -> project.getClassPaths()
        .stream()
        .map(v -> v.path.toString())
        .iterator();

    return origin.toBuilder()
        .setRootDir(project.rootPath.toString())
        .clearProductPaths()
        .addAllProductPaths(productPaths)
        .clearTestPaths()
        .addAllTestPaths(testPaths)
        .clearClassPaths()
        .addAllClassPaths(classPaths)
        .build();
  }

  private Strategies createStrategies(final Configuration configuration) {
    final FaultLocalization faultLocaliztion = new Ochiai();
    final JDTASTConstruction astConstruction = new JDTASTConstruction();
    final SourceCodeGeneration sourceCodeGeneration = new DefaultSourceCodeGeneration();
    final SourceCodeValidation sourceCodeValidation = new DefaultCodeValidation();
    final TestExecutor testExecutor = new LocalTestExecutor(configuration);
    final VariantSelection variantSelection = new GenerationalVariantSelection();
    return new Strategies(faultLocaliztion, astConstruction, sourceCodeGeneration,
        sourceCodeValidation, testExecutor, variantSelection);
  }

}
