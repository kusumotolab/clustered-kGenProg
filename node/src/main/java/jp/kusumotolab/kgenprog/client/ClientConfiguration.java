package jp.kusumotolab.kgenprog.client;

import com.electronwill.nightconfig.core.conversion.InvalidValueException;
import com.electronwill.nightconfig.core.conversion.PreserveNotNull;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.project.factory.TargetProjectFactory;
import jp.kusumotolab.kgenprog.project.factory.JUnitLibraryResolver.JUnitVersion;

public class ClientConfiguration extends Configuration {
  
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 50051;
  
  private ClientConfiguration(final Builder builder) {
    super(builder);
  }
  
  public static class Builder extends Configuration.Builder {
    
    @com.electronwill.nightconfig.core.conversion.Path("host")
    @PreserveNotNull
    private String host = DEFAULT_HOST;
    
    @com.electronwill.nightconfig.core.conversion.Path("port")
	@PreserveNotNull
	private int port = DEFAULT_PORT;
    
    private Builder() {
      super();
    }
	
    public static ClientConfiguration buildFromCmdLineArgs(final String[] args)
        throws IllegalArgumentException {
      
      final Builder builder = new Builder();
      final CmdLineParser parser = new CmdLineParser(builder);
       
      try {
        parser.parseArgument(args);
        final List<String> executionTestsFromCmdLine = builder.executionTests;
        final List<Path> classPathsFromCmdLine = builder.classPaths;

        if (needsParseConfigFile(args)) {
          builder.parseConfigFile();

          // Overwrite config values with ones from CLI
          parser.parseArgument(args);
          if (!executionTestsFromCmdLine.isEmpty()) {
            builder.executionTests.retainAll(executionTestsFromCmdLine);
          }
          if (!classPathsFromCmdLine.isEmpty()) {
            builder.classPaths.retainAll(classPathsFromCmdLine);
          }
        }

        validateArgument(builder);
      } catch (final CmdLineException | IllegalArgumentException | InvalidValueException
          | NoSuchFileException e) {
        // todo: make error message of InvalidValueException more user-friendly
        parser.printUsage(System.err);
        throw new IllegalArgumentException(e.getMessage());
      }

      return builder.build();
    }
    
    public ClientConfiguration build() {

      if (targetProject == null) {
        targetProject = TargetProjectFactory.create(rootDir, productPaths, testPaths, classPaths,
            JUnitVersion.JUNIT4);
      }

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
    
  }

}
