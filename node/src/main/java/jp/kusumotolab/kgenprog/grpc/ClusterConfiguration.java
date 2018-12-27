package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.PreserveNotNull;
import com.electronwill.nightconfig.core.file.FileConfig;

public class ClusterConfiguration {

  public static final int DEFAULT_PORT = 50051;

  private final Path workingDir;
  private final int port;

  private ClusterConfiguration(final Builder builder) {
    if (builder.workingDir == null) {
      try {
        workingDir = Files.createTempDirectory("kgenprog-coordinator");
        workingDir.toFile()
            .deleteOnExit();
      } catch (final IOException e) {
        throw new RuntimeException("Creating a temporary directory has failed.", e);
      }
    } else {
      workingDir = Paths.get(builder.workingDir);
    }

    port = builder.port;
  }

  public Path getWorkingDir() {
    return workingDir;
  }

  public int getPort() {
    return port;
  }

  public static class Builder {

    @PreserveNotNull
    private Path configPath = Paths.get("kgenprog-coordinator.toml");

    @com.electronwill.nightconfig.core.conversion.Path("working-dir")
    @PreserveNotNull
    private String workingDir;

    @com.electronwill.nightconfig.core.conversion.Path("port")
    @PreserveNotNull
    private int port = DEFAULT_PORT;


    public static ClusterConfiguration buildFromCmdLineArgs(final String[] args) {

      final Builder builder = new Builder();
      final CmdLineParser parser = new CmdLineParser(builder);

      try {
        parser.parseArgument(args);


        if (needsParseConfigFile(args)) {
          builder.parseConfigFile();

          // Overwrite config values with ones from CLI
          parser.parseArgument(args);
        }

      } catch (final CmdLineException | IllegalArgumentException | InvalidValueException
          | NoSuchFileException e) {
        // todo: make error message of InvalidValueException more user-friendly
        parser.printUsage(System.err);
        throw new IllegalArgumentException(e.getMessage());
      }

      return builder.build();
    }

    public Builder() {}

    public ClusterConfiguration build() {
      return new ClusterConfiguration(this);
    }

    public Builder setWorkingDir(final String workingDir) {
      this.workingDir = workingDir;
      return this;
    }

    public Builder setPort(final int port) {
      this.port = port;
      return this;
    }

    private static boolean needsParseConfigFile(final String[] args) {
      return args.length == 0 || Arrays.asList(args)
          .contains("--config");
    }

    private void parseConfigFile() throws InvalidValueException, NoSuchFileException {
      if (Files.notExists(configPath)) {
        throw new NoSuchFileException("config file \"" + configPath.toAbsolutePath()
            .toString() + "\" is not found.");
      }

      try (final FileConfig config = FileConfig.of(configPath)) {
        config.load();

        final ObjectConverter converter = new ObjectConverter();
        converter.toObject(config, this);
      }
    }

    @Option(name = "--config", metaVar = "<path>", usage = "Specifies the path to the config file.")
    private void setConfigPathFromCmdLineParser(final String configPath) {
      this.configPath = Paths.get(configPath);
    }

    @Option(name = "-w", aliases = "--working-dir", metaVar = "<path>",
        usage = "Specifies the path to working directory.")
    private void setWorkingDirFromCmdLineParser(final String workingDir) {
      this.workingDir = workingDir;
    }

    @Option(name = "--port", metaVar = "<num>",
        usage = "Port number waiting for connection from clients.")
    private void setRequiredSolutionsCountFromCmdLineParser(final int port) {
      this.port = port;
    }
  }

}
