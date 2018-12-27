package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jp.kusumotolab.kgenprog.project.ClassPath;
import jp.kusumotolab.kgenprog.project.factory.RawProjectFactory;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;

/**
 * TargetProjectのZipを行う
 * 
 * @author Ryo Arima
 *
 */
public class ProjectZipper {

  /**
   * ファイルIOバッファのサイズ
   */
  private static final int BUFFER_SIZE = 4048;

  /**
   * プロジェクト本体のファイルを格納するディレクトリ
   */
  public static final Path PROJECT_PREFIX = Paths.get("project");

  /**
   * 依存関係のファイルを格納するディレクトリ
   */
  public static final Path CLASSPATH_PREFIX = Paths.get("classes");


  /**
   * TargetProjectに含まれるファイルをZIPファイル形式として1つにまとめる
   * 
   * 生成されるZIPファイルは以下のような構造となる
   * <ul>
   * <li>{@link TargetProject#rootPath} 以下のディレクトリは {@link #PROJECT_PREFIX}以下に格納される</li>
   * <li>{@link TargetProject#getClassPaths()} のファイルは {@link #CLASSPATH_PREFIX}以下に格納される</li>
   * <ul>
   * <li>クラスパスが単一ファイルの場合、そのままのファイル名で格納される</li>
   * <li>クラスパスがディレクトリの場合、ディレクトリ名は数字に変換される。ディレクトリ内の構造は維持される</li>
   * </ul>
   * </ul>
   * 
   * 生成されたZIPファイルは{@code stream}から取得される{@link OutputStream}に書き込まれる
   *
   * @param project ZIP対象プロジェクト
   * @param stream 書き込み先ストリーム
   * @return 各パスをZIPファイル内のパスへ変換した{@link TargetProject}
   */
  public static TargetProject zipProject(final TargetProject project,
      final Supplier<OutputStream> stream) throws IOException {
    try {
      return new ProjectZipper(project, stream).zip();
    } catch (final UncheckedIOException e) {
      throw new IOException(e.getCause());
    }
  }

  private final TargetProject project;
  private final Supplier<OutputStream> stream;

  private final Path root;
  private ZipOutputStream zipOutputStream;
  private final byte[] buffer;
  private final List<Path> classPaths;

  private ProjectZipper(final TargetProject project, final Supplier<OutputStream> stream)
      throws IOException {
    this.project = project;
    this.stream = stream;

    root = project.rootPath.toRealPath();

    buffer = new byte[BUFFER_SIZE];
    classPaths = new ArrayList<>();
  }

  private TargetProject zip() throws IOException {
    try (final ZipOutputStream zos = new ZipOutputStream(stream.get())) {
      zipOutputStream = zos;

      writeProjectFiles();
      writeClassPaths();
    }

    final List<Path> productPaths = convertProductSourcePaths();
    final List<Path> testPaths = convertTestSourcePaths();

    return new RawProjectFactory(Paths.get("."), productPaths, testPaths, classPaths).create();
  }

  /**
   * rootディレクトリ以下にあるプロジェクト本体のファイルをZipOutputStreamに書き込む
   */
  private void writeProjectFiles() throws IOException {
    try (final Stream<Path> paths = Files.walk(root)) {
      paths.filter(p -> !Files.isDirectory(p))
          .forEach(p -> writeEachFile(p, createProjectPath(p)));
    }
  }

  /**
   * 依存ファイルをZipOutputStreamに書き込む
   */
  private void writeClassPaths() throws IOException {
    final AtomicInteger classDirCnt = new AtomicInteger(0);
    for (final ClassPath cp : project.getClassPaths()) {
      final Path classPath = cp.path.toRealPath();

      if (isDescendant(root, classPath)) {
        // rootディレクトリ以下はコピー済み
        classPaths.add(createProjectPath(classPath));


      } else if (Files.isDirectory(classPath)) {
        // クラスディレクトリ
        try (final Stream<Path> paths = Files.walk(classPath)) {
          paths.filter(p -> !Files.isDirectory(p))
              .forEach(p -> {
                // ディレクトリ名が重複する可能性があるので数字ディレクトリにする
                final int number = classDirCnt.getAndIncrement();
                final Path path = createDirectoryClassPath(classPath, number, p);
                classPaths.add(path);
                writeEachFile(p, path);
              });
        }

      } else {
        // 単一のファイル（jarなど）
        final Path path = createSingleClassPath(classPath);
        classPaths.add(path);
        writeEachFile(classPath, path);
      }
    }
  }

  /**
   * 1つのファイルをZipOutputStreamに書き込む
   * 
   * @param realPath 実際のファイルへのパス
   * @param zipPath ZIPファイル中でのパス
   */
  private void writeEachFile(final Path realPath, final Path zipPath) {
    final String zipPathStr = pathToString(zipPath);
    try (final InputStream inputStream = Files.newInputStream(realPath)) {
      final ZipEntry entry = new ZipEntry(zipPathStr);
      zipOutputStream.putNextEntry(entry);

      int count;
      while ((count = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
        zipOutputStream.write(buffer, 0, count);
      }
      zipOutputStream.closeEntry();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * プロジェクト本体ファイルのパスを、ZIPファイル中でのパスに変換する
   */
  private Path createProjectPath(final Path target) {
    final Path relativePath = root.relativize(target);
    final Path prefixPath = PROJECT_PREFIX.resolve(relativePath);
    return prefixPath;
  }

  /**
   * 依存ファイルへのパスを、ZIPファイル中でのパスに変換する
   */
  private Path createSingleClassPath(final Path target) {
    final Path prefixPath = CLASSPATH_PREFIX.resolve(target.getFileName());
    return prefixPath;
  }

  /**
   * 依存ディレクトリへのパスを、ZIPファイル中でのパスに変換する
   */
  private Path createDirectoryClassPath(final Path classDir, final int id, final Path target) {
    final Path relativePath = classDir.relativize(target);
    final Path prefixPath = CLASSPATH_PREFIX.resolve(Integer.toString(id))
        .resolve(relativePath);
    return prefixPath;
  }

  /**
   * ProductSourcePathsをZIPファイル中でのパスに変換する
   */
  private List<Path> convertProductSourcePaths() {
    return project.getProductSourcePaths()
        .stream()
        .map(v -> {
          try {
            return createProjectPath(v.path.toRealPath());
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        })
        .collect(Collectors.toList());
  }

  /**
   * TestSourcePathsをZIPファイル中でのパスに変換する
   */
  private List<Path> convertTestSourcePaths() {
    return project.getTestSourcePaths()
        .stream()
        .map(v -> {
          try {
            return createProjectPath(v.path.toRealPath());
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        })
        .collect(Collectors.toList());
  }

  /**
   * パスを\'/\'区切りの文字列に変換する
   */
  private String pathToString(final Path path) {
    final String result = path.toString();
    final String separator = path.getFileSystem()
        .getSeparator();
    if (separator.equals("/")) {
      return result;
    }

    return path.toString()
        .replace(separator, "/");
  }

  /**
   * targetがrootディレクトリ以下にあるかを確認する
   */
  private static boolean isDescendant(final Path root, final Path target) throws IOException {
    return root.startsWith(target);
  }
}
