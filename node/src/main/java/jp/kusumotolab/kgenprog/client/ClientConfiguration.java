package jp.kusumotolab.kgenprog.client;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import jp.kusumotolab.kgenprog.Configuration;

/**
 * kGenProg のオプションに {@code --host <host>} と {@code --port <port>} を追加したもの．
 * 従来の kGenProg のオプションは {@code --kgp-args <args...>} として記述する．
 * <br>
 * Note: 現状 kGenProg オプションにスペースが入っているとバグる．
 * <pre>
 * --kgp-args '--config "/path/containing/space/kGenProg settings.toml"'
 *                                                      ^ Cannot use spaces!
 * </pre>
 * 
 * @author h-matsuo
 *
 */
public class ClientConfiguration {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 50051;

  private final Configuration config;
  private final String host;
  private final int port;

  protected ClientConfiguration(final Builder builder) {
    config = builder.config;
    host = builder.host;
    port = builder.port;
  }

  public Configuration getConfig() {
    return config;
  }
  
  public String getHost() {
	  return host;
  }
  
  public int getPort() {
	  return port;
  }

  public static class Builder {
    
    private Configuration config;    
    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private String[] kgpArgs;

    private Builder() {}

    public static ClientConfiguration buildFromCmdLineArgs(final String[] args)
        throws IllegalArgumentException {

      final Builder builder = new Builder();
      final CmdLineParser parser = new CmdLineParser(builder);

      try {
        parser.parseArgument(args);
        builder.config = Configuration.Builder.buildFromCmdLineArgs(builder.kgpArgs);
      } catch (final CmdLineException | IllegalArgumentException | InvalidValueException e) {
        // todo: make error message of InvalidValueException more user-friendly
        parser.printUsage(System.err);
        throw new IllegalArgumentException(e.getMessage());
      }

      return builder.build();
    }

    public ClientConfiguration build() {
      return new ClientConfiguration(this);
    }
    
    @Option(name = "--host", metaVar = "<host>",
        usage = "Host where coordinator is running.")
    private void setHostFromCmdLineParser(final String host) {
      this.host = host;
    }
    
    @Option(name = "--port", metaVar = "<port>",
        usage = "Port number where coordinator is listening.")
    private void setPortNumberFromCmdLineParser(final int port) {
      this.port = port;
    }
    
    @Option(name = "--kgp-args", metaVar = "<kGenProg's args...>",
        usage = "kGenProg's arguments.")
    private void setKgpArgsFromCmdLineParser(final String kgpArgs) {
      this.kgpArgs = kgpArgs.split(" ");
    }
    
  }
  
}

