package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.testutil.ExampleAlias.Src;


public class ProjectZipperTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void test() throws IOException {
    final Path rootPath = Paths.get("../main/example/BuildSuccess01");
    final TargetProject targetProject = TargetProjectFactory.create(rootPath);
    final Path originFoo = targetProject.getProductSourcePaths()
        .get(0)
        .getResolvedPath();
    final Path originFooTest = targetProject.getTestSourcePaths()
        .get(0)
        .getResolvedPath();
    final Path originJUnit = targetProject.getClassPaths()
        .stream()
        .filter(v -> v.path.endsWith("junit-4.12.jar"))
        .findFirst()
        .get().path;
    final Path originHamcrest = targetProject.getClassPaths()
        .stream()
        .filter(v -> v.path.endsWith("hamcrest-core-1.3.jar"))
        .findFirst()
        .get().path;

    // TargetProjectをzipする
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final TargetProject zip = ProjectZipper.zipProject(targetProject, () -> outputStream);

    final Path zipJUnit = ProjectZipper.CLASSPATH_PREFIX.resolve("junit-4.12.jar");
    final Path zipHamcrest = ProjectZipper.CLASSPATH_PREFIX.resolve("hamcrest-core-1.3.jar");

    // 変換されたTargetProjectの確認
    assertThat(zip.rootPath).isEqualTo(ProjectZipper.PROJECT_PREFIX);
    assertThat(zip.getProductSourcePaths()).extracting(v -> v.path)
        .containsExactly(Src.FOO);
    assertThat(zip.getTestSourcePaths()).extracting(v -> v.path)
        .containsExactly(Src.FOO_TEST);
    assertThat(zip.getClassPaths()).extracting(v -> v.path)
        .contains(zipJUnit, zipHamcrest);

    // Unzipする
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final Path destination = tempFolder.newFolder()
        .toPath();
    final TargetProject unzip = ProjectUnzipper.unzipProject(destination, zip, () -> inputStream);

    final Path unzipFoo = unzip.rootPath.resolve("src/example/Foo.java");
    final Path unzipFooTest = unzip.rootPath.resolve("src/example/FooTest.java");
    final Path unzipJUnit = destination.resolve(ProjectZipper.CLASSPATH_PREFIX)
        .resolve("junit-4.12.jar");
    final Path unzipHamcrest = destination.resolve(ProjectZipper.CLASSPATH_PREFIX)
        .resolve("hamcrest-core-1.3.jar");

    // 変換されたTargetProjectの確認
    assertThat(unzip.rootPath).isEqualTo(destination.resolve(ProjectZipper.PROJECT_PREFIX));
    assertThat(unzip.getProductSourcePaths()).extracting(v -> v.path)
        .containsExactly(Src.FOO);
    assertThat(unzip.getTestSourcePaths()).extracting(v -> v.path)
        .containsExactly(Src.FOO_TEST);
    assertThat(unzip.getClassPaths()).extracting(v -> v.path)
        .contains(unzipJUnit, unzipHamcrest);

    // ファイルの内容がコピーされているか確認
    assertSameFile(originFoo, unzipFoo);
    assertSameFile(originFooTest, unzipFooTest);
    assertSameFile(originJUnit, unzipJUnit);
    assertSameFile(originHamcrest, unzipHamcrest);
  }

  private void assertSameFile(final Path expect, final Path target) throws IOException {
    assertThat(Files.readAllBytes(target)).isEqualTo(Files.readAllBytes(expect));
  }

}
