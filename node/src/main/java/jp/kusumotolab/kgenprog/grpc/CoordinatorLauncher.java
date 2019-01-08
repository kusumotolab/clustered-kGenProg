package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.kusumotolab.kgenprog.coordinator.Coordinator;
import jp.kusumotolab.kgenprog.coordinator.worker.LocalWorker;

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
    final Worker worker = new LocalWorker(config.getWorkingDir());
    final Coordinator coordinator = new Coordinator(config, worker);
    coordinator.start();
  }

}
