package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;

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
    final Worker worker = new LocalWorker();
    final Coordinator coordinator = new Coordinator(config, worker);
    coordinator.start();
  }

}
