package jp.kusumotolab.kgenprog.project.factory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import jp.kusumotolab.kgenprog.project.ClassPath;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.TestSourcePath;

public class RawProjectFactory implements ProjectFactory {

  private final Path rootPath;
  private final List<ProductSourcePath> productSourcePaths;
  private final List<TestSourcePath> testSourcePaths;
  private final List<ClassPath> classPaths;

  public RawProjectFactory(final Path rootPath, final List<Path> pathsForProductSource,
      final List<Path> pathsForTestSource, final List<Path> pathsForClass) {
    this.rootPath = rootPath;
    this.productSourcePaths = pathsForProductSource.stream()
        .map(ProductSourcePath::new)
        .collect(Collectors.toList());
    this.testSourcePaths = pathsForTestSource.stream()
        .map(TestSourcePath::new)
        .collect(Collectors.toList());
    this.classPaths = pathsForClass.stream()
        .map(ClassPath::new)
        .collect(Collectors.toList());
  }

  @Override
  public TargetProject create() {
    return new TargetProject(rootPath, productSourcePaths, testSourcePaths, classPaths);
  }

  @Override
  public boolean isApplicable() {
    return true;
  }
}
