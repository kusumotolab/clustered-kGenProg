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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import jp.kusumotolab.kgenprog.project.ClassPath;
import jp.kusumotolab.kgenprog.project.factory.RawProjectFactory;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;

public class ProjectZipper {

  private static final int BUFFER = 4048;
  private static final long FILE_SIZE_LIMIT = 1L << 30;
  private static final int ENTRY_NUMBER_LIMIT = 100000;
  public static final Path PROJECT_PREFIX = Paths.get(".", "project");
  public static final Path CLASSPATH_PREFIX = Paths.get(".", "classes");



  public TargetProject zipProject(final TargetProject project, final Supplier<OutputStream> stream)
      throws IOException {

    final List<Path> productPaths;
    final List<Path> testPaths;
    final List<Path> classPaths = new ArrayList<>();
    final Path zipRootPath = Paths.get(".");
    final Path root = project.rootPath.toRealPath();
    try (ZipOutputStream zos = new ZipOutputStream(stream.get())) {
      // プロジェクト本体の書き込み
      try (final Stream<Path> paths = Files.walk(root)) {
        paths.filter(p -> !Files.isDirectory(p))
            .forEach(p -> zipEachFile(zos, p, pathToString(createProjectPath(root, p))));
      }
      final AtomicInteger classDirCnt = new AtomicInteger(0);
      // 依存ファイルの書き込み
      for (final ClassPath cp : project.getClassPaths()) {
        final Path classPath = cp.path.toRealPath();
        if (isChild(root, classPath)) {
          // rootディレクトリ以下はコピー済み
          classPaths.add(createProjectPath(root, classPath));
          continue;
        }

        if (Files.isDirectory(classPath)) {
          // クラスフォルダ
          try (final Stream<Path> paths = Files.walk(classPath)) {
            paths.filter(p -> !Files.isDirectory(p))
                .forEach(p -> {
                  final Path path =
                      createDirectoryClassPath(root, classPath, classDirCnt.getAndIncrement(), p);
                  classPaths.add(path);
                  zipEachFile(zos, p, pathToString(path));
                });
          }

        } else {
          // 単一のファイル（jarなど）
          final Path path = createSingleClassPath(root, classPath);
          classPaths.add(path);
          zipEachFile(zos, classPath, pathToString(path));
        }
      }


      productPaths = project.getProductSourcePaths()
          .stream()
          .map(v -> {
            try {
              return createProjectPath(root, v.path.toRealPath());
            } catch (final IOException e) {
              throw new UncheckedIOException(e);
            }
          })
          .collect(Collectors.toList());

      testPaths = project.getTestSourcePaths()
          .stream()
          .map(v -> {
            try {
              return createProjectPath(root, v.path.toRealPath());
            } catch (final IOException e) {
              throw new UncheckedIOException(e);
            }
          })
          .collect(Collectors.toList());

    } catch (final UncheckedIOException e) {
      throw e.getCause();
    }

    return new RawProjectFactory(zipRootPath, productPaths, testPaths, classPaths).create();
  }

  private void zipEachFile(final ZipOutputStream zos, final Path realPath, final String zipPath) {
    final byte[] data = new byte[BUFFER];
    try (InputStream inputStream = Files.newInputStream(realPath)) {
      final ZipEntry entry = new ZipEntry(zipPath);
      zos.putNextEntry(entry);

      int count;
      while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
        zos.write(data, 0, count);
      }
      zos.closeEntry();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path createProjectPath(final Path root, final Path target) {
    final Path relativePath = root.relativize(target);
    final Path prefixPath = PROJECT_PREFIX.resolve(relativePath);
    return prefixPath;
  }

  private Path createSingleClassPath(final Path root, final Path target) {
    // jarファイルは1つのディレクトリに全部入れる
    final Path prefixPath = CLASSPATH_PREFIX.resolve(target.getFileName());
    return prefixPath;
  }

  private Path createDirectoryClassPath(final Path root, final Path classDir, final int classDirCnt,
      final Path target) {
    // ディレクトリクラスパスは名前が衝突する可能性があるので数字にする
    final Path relativePath = classDir.relativize(target);
    final Path prefixPath = CLASSPATH_PREFIX.resolve(Integer.toString(classDirCnt))
        .resolve(relativePath);
    return prefixPath;
  }

  private boolean isChild(final Path root, final Path target) throws IOException {
    return root.startsWith(target);
  }

  private String pathToString(final Path path) {
    return path.toString()
        .replace('\\', '/');
  }

  public static void main(final String[] args) throws IOException {
    Files.walk(Paths.get(".")
        .toRealPath())
        .forEach(System.out::println);
  }

  long sizeCnt;

  public TargetProject unzipProject(final Path destination, final TargetProject project,
      final Supplier<InputStream> stream) throws IOException {
    int entryCnt = 0;
    sizeCnt = 0;
    try (ZipInputStream zis = new ZipInputStream(stream.get())) {
      while (unzipEachEntry(zis, zis.getNextEntry(), destination)) {
        entryCnt++;
        if (entryCnt > ENTRY_NUMBER_LIMIT) {
          throw new IOException("Too many files to unzip.");
        }
      }
    }

    final List<Path> productPaths = project.getProductSourcePaths()
        .stream()
        .map(v -> destination.resolve(v.path))
        .collect(Collectors.toList());

    final List<Path> testPaths = project.getTestSourcePaths()
        .stream()
        .map(v -> destination.resolve(v.path))
        .collect(Collectors.toList());

    final List<Path> classPaths = project.getClassPaths()
        .stream()
        .map(v -> destination.resolve(v.path))
        .collect(Collectors.toList());

    return new RawProjectFactory(destination, productPaths, testPaths, classPaths).create();
  }

  private boolean unzipEachEntry(final ZipInputStream zis, final ZipEntry entry,
      final Path rootPath) throws IOException {
    if (entry == null) {
      return false;
    }
    if (entry.isDirectory()) {
      return true;
    }

    final Path filePath = createFilePath(rootPath, entry.getName());
    Files.createDirectories(filePath.getParent());

    final byte[] data = new byte[BUFFER];
    try (OutputStream dest = Files.newOutputStream(filePath)) {
      int count;
      while ((count = zis.read(data, 0, BUFFER)) != -1) {
        sizeCnt += count;
        if (sizeCnt > FILE_SIZE_LIMIT) {
          throw new IOException("File being unzipped is too big.");
        }
        dest.write(data, 0, count);
      }
      zis.closeEntry();
    }

    return true;
  }

  private Path createFilePath(final Path rootPath, final String filename) throws IOException {
    final Path realFilePath = rootPath.resolve(filename)
        .normalize();
    if (!realFilePath.startsWith(rootPath)) {
      throw new IOException("File is outside extraction target directory.");
    }
    return realFilePath;
  }

}
