package jp.kusumotolab.kgenprog.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.worker.WorkerConfiguration.Builder;

public class WorkerLauncher {

  private static final Logger log = LoggerFactory.getLogger(WorkerLauncher.class);

  public static void main(final String[] args) {
    final WorkerLauncher workerLauncher = new WorkerLauncher();
    final WorkerConfiguration workerConfiguration = Builder.buildFromCmdLineArgs(args);
    workerLauncher.launch(workerConfiguration);
  }

  private void launch(final WorkerConfiguration configuration) {
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(configuration.getHost(),
        configuration.getPort())
        .usePlaintext()
        .build();

    final int freePort = getFreePort();
    final String name = getHostAddress();
    new CoordinatorClient(managedChannel).registerWorker(name, freePort);

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

}
