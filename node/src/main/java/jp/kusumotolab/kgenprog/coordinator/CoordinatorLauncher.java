package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.kusumotolab.kgenprog.coordinator.worker.LocalWorker;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.Worker;

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
    final Path workerDir = config.getWorkingDir()
        .resolve("worker1");
    Files.createDirectories(workerDir);
    final Coordinator coordinator = new Coordinator(config);
    coordinator.start();
  }

}
