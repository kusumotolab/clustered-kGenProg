package jp.kusumotolab.kgenprog.coordinator;

import java.io.IOException;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;

public class CoordinatorLauncher {

  public static void main(final String[] args) throws IOException, InterruptedException {
    final ClusterConfiguration config;
    final CoordinatorLauncher launcher = new CoordinatorLauncher();
    try {
      config = ClusterConfiguration.Builder.buildFromCmdLineArgs(args);
    } catch (final IllegalArgumentException e) {
      System.exit(1);
      return;
    }
    launcher.launch(config);
  }

  public void launch(final ClusterConfiguration config) throws IOException, InterruptedException {
    final Coordinator coordinator = new Coordinator(config);
    coordinator.start();
  }
}
