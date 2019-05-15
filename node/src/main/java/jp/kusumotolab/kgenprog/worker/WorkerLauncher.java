package jp.kusumotolab.kgenprog.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jp.kusumotolab.kgenprog.grpc.ClusterConfiguration;
import jp.kusumotolab.kgenprog.grpc.Worker;
import jp.kusumotolab.kgenprog.worker.WorkerConfiguration.Builder;

public class WorkerLauncher {

  public static void main(final String[] args) {
    final WorkerLauncher workerLauncher = new WorkerLauncher();
    final WorkerConfiguration workerConfiguration = Builder.buildFromCmdLineArgs(args);
    workerLauncher.launch(workerConfiguration);
  }

  private void launch(final WorkerConfiguration configuration) {
    final ManagedChannel managedChannel =
        ManagedChannelBuilder.forAddress(configuration.getHost(), configuration.getPort())
            .usePlaintext()
            .keepAliveTime(ClusterConfiguration.DEFAULT_KEEPALIVE_SECONDS, TimeUnit.SECONDS)
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();

    final CoordinatorClient coordinatorClient = new CoordinatorClient(managedChannel);
    final int freePort = configuration.getWorkerPort();

    final Path path = Paths.get("worker-" + freePort);
    try {
      Files.createDirectory(path);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final Worker worker = new LocalWorker(0, path, coordinatorClient);
    final WorkerService workerService = new WorkerService(worker);

    final Server server = ServerBuilder.forPort(freePort)
        .addService(workerService)
        .executor(Executors.newSingleThreadExecutor())
        .maxInboundMessageSize(Integer.MAX_VALUE)
        .build();
    try {
      server.start();
      coordinatorClient.registerWorker(freePort);
      server.awaitTermination();
    } catch (final IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
