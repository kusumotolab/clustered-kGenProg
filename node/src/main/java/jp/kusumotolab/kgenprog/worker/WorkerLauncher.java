package jp.kusumotolab.kgenprog.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jp.kusumotolab.kgenprog.grpc.Worker;
import jp.kusumotolab.kgenprog.worker.WorkerConfiguration.Builder;

public class WorkerLauncher {

  public static void main(final String[] args) {
    final WorkerLauncher workerLauncher = new WorkerLauncher();
    final WorkerConfiguration workerConfiguration = Builder.buildFromCmdLineArgs(args);
    workerLauncher.launch(workerConfiguration);
  }

  private void launch(final WorkerConfiguration configuration) {
    setLogLevel(Level.DEBUG);
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(configuration.getHost(),
        configuration.getPort())
        .usePlaintext()
        .build();

    final CoordinatorClient coordinatorClient = new CoordinatorClient(managedChannel);

    final int freePort = getFreePort();
    final String name = getHostAddress();
    coordinatorClient.registerWorker(name, freePort);

    final Path path = Paths.get("worker-" + freePort);
    try {
      Files.createDirectory(path);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final Worker worker = new LocalWorker(path, coordinatorClient);
    final WorkerService workerService = new WorkerService(worker);

    final Server server = ServerBuilder.forPort(freePort)
        .addService(workerService)
        .build();
    try {
      server.start();
      server.awaitTermination();
    } catch (final IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private int getFreePort() {
    final int port;
    try (Socket socket = new Socket()) {
      socket.bind(null);
      port = socket.getLocalPort();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return port;
  }

  private String getHostAddress() {
    try {
      final InetAddress address = InetAddress.getLocalHost();
      return address.getHostAddress();
    } catch (final UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private void setLogLevel(final Level logLevel) {
    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(logLevel);
  }


}
