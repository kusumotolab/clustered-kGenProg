package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jp.kusumotolab.kgenprog.project.factory.RawProjectFactory;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;

/**
 * TargetProjectのUnzipを行う
 * 
 * @author Ryo Arima
 *
 */
public class ProjectUnzipper {

  /**
   * ファイルIOバッファのサイズ
   */
  private static final int BUFFER_SIZE = 4048;

  /**
   * ZIPファイルが展開されたときの最大ファイルサイズ
   */
  private static final long FILE_SIZE_LIMIT = 1L << 30;

  /**
   * ZIPファイルが展開されたときの最大ファイル数
   */
  private static final int ENTRY_NUMBER_LIMIT = 100000;


  /**
   * {@link ProjectZipper#zipProject(TargetProject, Supplier)} によって生成されたZIPファイルを展開する
   * 
   * @param destination 展開先ディレクトリ
   * @param project 展開対象プロジェクト
   * @param stream ZIPファイル読み込みストリーム
   * @return 各パスを展開先ディレクトリでのパスに変換した{@link TargetProject}
   * @throws IOException ファイル数やファイルサイズが制限を超えたとき。展開先ディレクトリ以外にファイルが展開されたとき。
   */
  public static TargetProject unzipProject(final Path destination, final TargetProject project,
      final Supplier<InputStream> stream) throws IOException {
    try {
      return new ProjectUnzipper(destination, project, stream).unzip();
    } catch (final UncheckedIOException e) {
      throw new IOException(e.getCause());
    }
  }

  private final Path destination;
  private final TargetProject project;
  private final Supplier<InputStream> stream;

  private ZipInputStream zipInputStream;
  private final byte[] buffer;
  private long sizeCnt;

  private ProjectUnzipper(final Path destination, final TargetProject project,
      final Supplier<InputStream> stream) {
    this.destination = destination;
    this.project = project;
    this.stream = stream;

    buffer = new byte[BUFFER_SIZE];
  }

  private TargetProject unzip() throws IOException {
    int entryCnt = 0;
    try (final ZipInputStream zis = new ZipInputStream(stream.get())) {
      zipInputStream = zis;
      while (readEachEntry(zis.getNextEntry())) {
        entryCnt++;
        if (entryCnt > ENTRY_NUMBER_LIMIT) {
          throw new IOException("Too many files to unzip.");
        }
      }
    }

    final List<Path> productPaths = project.getProductSourcePaths()
        .stream()
        .map(v -> v.path)
        .collect(Collectors.toList());

    final List<Path> testPaths = project.getTestSourcePaths()
        .stream()
        .map(v -> v.path)
        .collect(Collectors.toList());

    final List<Path> classPaths = project.getClassPaths()
        .stream()
        .map(v -> Paths.get(".")
            .resolve(destination)
            .resolve(v.path))
        .collect(Collectors.toList());
    return new RawProjectFactory(destination.resolve(ProjectZipper.PROJECT_PREFIX), productPaths,
        testPaths, classPaths).create();
  }

  /**
   * 1つのファイルを読み込み、展開先ディレクトリへ展開する
   */
  private boolean readEachEntry(final ZipEntry entry) throws IOException {
    if (entry == null) {
      return false;
    }
    if (entry.isDirectory()) {
      return true;
    }

    final Path filePath = createFilePath(entry.getName());
    Files.createDirectories(filePath.getParent());

    try (final OutputStream dest = Files.newOutputStream(filePath)) {
      int count;
      while ((count = zipInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
        sizeCnt += count;
        if (sizeCnt > FILE_SIZE_LIMIT) {
          throw new IOException("File being unzipped is too big.");
        }
        dest.write(buffer, 0, count);
      }
      zipInputStream.closeEntry();
    }

    return true;
  }

  /**
   * ZIPファイルパスを展開先パスに変換する
   * 
   * @throws IOException 変換結果が展開先ディレクトリの外になってしまう場合
   */
  private Path createFilePath(final String filepath) throws IOException {
    final Path realFilePath = destination.resolve(filepath)
        .normalize();
    if (!realFilePath.startsWith(destination)) {
      throw new IOException("File is outside extraction target directory.");
    }
    return realFilePath;
  }
}
