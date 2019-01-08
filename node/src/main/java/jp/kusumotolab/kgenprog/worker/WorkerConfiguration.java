package jp.kusumotolab.kgenprog.worker;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import jp.kusumotolab.kgenprog.Configuration;

public class WorkerConfiguration {

  public static final String DEFAULT_WORKER_HOST = "localhost";
  public static final int DEFAULT_WORKER_PORT = 50051;
  public static final String DEFAULT_COORDINATOR_HOST = "localhost";
  public static final int DEFAULT_COORDINATOR_PORT = 50052;

  private final String workerHost;
  private final int workerPort;
  private final String coordinatorHost;
  private final int coordinatorPort;

  protected WorkerConfiguration(
      final WorkerConfiguration.Builder builder) {
    workerHost = builder.workerHost;
    workerPort = builder.workerPort;
    coordinatorHost = builder.coordinatorHost;
    coordinatorPort = builder.coordinatorPort;
  }

  public String getWorkerHost() {
    return workerHost;
  }

  public int getWorkerPort() {
    return workerPort;
  }

  public String getCoordinatorHost() {
    return coordinatorHost;
  }

  public int getCoordinatorPort() {
    return coordinatorPort;
  }

  public static class Builder {


    private String workerHost = DEFAULT_WORKER_HOST;
    private int workerPort = DEFAULT_WORKER_PORT;
    private String coordinatorHost = DEFAULT_COORDINATOR_HOST;
    private int coordinatorPort = DEFAULT_WORKER_PORT;

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

    @Option(name = "--w-host", metaVar = "<host>",
        usage = "Host where worker is running.")
    private void setWorkerHostFromCmdLineParser(final String host) {
      this.workerHost = host;
    }

    @Option(name = "--w-port", metaVar = "<port>",
        usage = "Port number where worker is listening.")
    private void setWorkerPortNumberFromCmdLineParser(final int port) {
      this.workerPort = port;
    }



    @Option(name = "--c-host", metaVar = "<host>",
        usage = "Host where coordinator is running.")
    private void setCoordinatorHostFromCmdLineParser(final String host) {
      this.coordinatorHost = host;
    }

    @Option(name = "--c-port", metaVar = "<port>",
        usage = "Port number where coordinator is listening.")
    private void setCoordinatorPortNumberFromCmdLineParser(final int port) {
      this.coordinatorPort = port;
    }

  }

}
