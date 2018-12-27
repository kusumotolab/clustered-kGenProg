package jp.kusumotolab.kgenprog.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Paths;
import org.junit.Test;


public class ClusterConfigurationBuilderTest {

  @Test
  public void testConfigurationFromCmdLine() {
    final ClusterConfiguration configuration = ClusterConfiguration.Builder.buildFromCmdLineArgs(
        new String[] {"--working-dir", "./kgenprog/cluster", "--port", "1024"});

    assertThat(configuration.getWorkingDir()).isEqualTo(Paths.get("./kgenprog/cluster"));
    assertThat(configuration.getPort()).isEqualTo(1024);
  }

  @Test
  public void testConfigurationDefaultValue() {
    final ClusterConfiguration configuration = new ClusterConfiguration.Builder().build();

    assertThat(configuration.getWorkingDir())
        .startsWith(Paths.get(System.getProperty("java.io.tmpdir")));
    assertThat(configuration.getPort()).isEqualTo(ClusterConfiguration.DEFAULT_PORT);

  }
}
