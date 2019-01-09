package jp.kusumotolab.kgenprog.worker;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import com.electronwill.nightconfig.core.conversion.InvalidValueException;

public class WorkerConfiguration {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 50051;

  private final String host;
  private final int port;

  protected WorkerConfiguration(
      final WorkerConfiguration.Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public static class Builder {

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;

    private Builder() {
    }

    public static WorkerConfiguration buildFromCmdLineArgs(final String[] args)
        throws IllegalArgumentException {

      final WorkerConfiguration.Builder builder = new WorkerConfiguration.Builder();
      final CmdLineParser parser = new CmdLineParser(builder);

      try {
        parser.parseArgument(args);
      } catch (final CmdLineException | IllegalArgumentException | InvalidValueException e) {
        // todo: make error message of InvalidValueException more user-friendly
        parser.printUsage(System.err);
        throw new IllegalArgumentException(e.getMessage());
      }
      return builder.build();
    }

    public WorkerConfiguration build() {
      return new WorkerConfiguration(this);
    }

    @Option(name = "--host", metaVar = "<host>",
        usage = "Host where coordinator is running.")
    private void setHostFromCmdLineParser(final String host) {
      this.host = host;
    }

    @Option(name = "--port", metaVar = "<port>",
        usage = "Port number where coordinator is listening.")
    private void setWorkerPortNumberFromCmdLineParser(final int port) {
      this.port = port;
    }
  }

}
