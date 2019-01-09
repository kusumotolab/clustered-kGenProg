package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;

public class CoordinatorLauncher {

  public static void main(final String[] args) throws IOException, InterruptedException {
    final ClusterConfiguration config;
    final CoordinatorLauncher launcher;
    try {
      config = ClusterConfiguration.Builder.buildFromCmdLineArgs(args);
      launcher = new CoordinatorLauncher();
    } catch (final IllegalArgumentException e) {
      System.exit(1);
      return;
    }
    launcher.launch(config);
  }

  public void launch(final ClusterConfiguration config) throws IOException, InterruptedException {
    setLogLevel(Level.DEBUG);
    final Path workerDir = config.getWorkingDir()
        .resolve("worker1");
    Files.createDirectories(workerDir);
    final Coordinator coordinator = new Coordinator(config);
    coordinator.start();
  }


  private void setLogLevel(final Level logLevel) {
    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(logLevel);
  }
}
