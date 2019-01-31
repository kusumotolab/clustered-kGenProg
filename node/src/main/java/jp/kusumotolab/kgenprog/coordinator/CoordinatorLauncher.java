package jp.kusumotolab.kgenprog.coordinator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
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
    setLogConfig(config);
    final Coordinator coordinator = new Coordinator(config);
    coordinator.start();
  }

  private void setLogConfig(final ClusterConfiguration config) {
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    try {
      final JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      //context.reset();
      final InputStream inputStream = createLogConfigXML(config.getLogDestination());
      configurator.doConfigure(inputStream);
    } catch (final JoranException e) {
      e.printStackTrace();
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);

  }

  private InputStream createLogConfigXML(final String destination) {
    final String configXML = new StringBuilder() //
        .append("<configuration>")
        .append(
            "  <appender name=\"stash\" class=\"net.logstash.logback.appender.LogstashTcpSocketAppender\">")
        .append("    <destination>")
        .append(destination)
        .append("    </destination>")
        .append("    <encoder class=\"net.logstash.logback.encoder.LogstashEncoder\" />")
        .append("  </appender>")
        .append("  <logger name=\"jp.kusumotolab.kgenprog.coordinator.log.Events\" level=\"info\">")
        .append("    <appender-ref ref=\"stash\" />")
        .append("  </logger>")
        .append("</configuration>")
        .toString();
    return new ByteArrayInputStream(configXML.getBytes());
  }
}
