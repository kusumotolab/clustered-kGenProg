package jp.kusumotolab.kgenprog.client;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.KGenProgMain;
import jp.kusumotolab.kgenprog.fl.FaultLocalization;
import jp.kusumotolab.kgenprog.fl.Ochiai;
import jp.kusumotolab.kgenprog.ga.codegeneration.DefaultSourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.codegeneration.SourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.crossover.Crossover;
import jp.kusumotolab.kgenprog.ga.crossover.SinglePointCrossover;
import jp.kusumotolab.kgenprog.ga.mutation.Mutation;
import jp.kusumotolab.kgenprog.ga.mutation.RandomMutation;
import jp.kusumotolab.kgenprog.ga.mutation.selection.RouletteStatementSelection;
import jp.kusumotolab.kgenprog.ga.selection.DefaultVariantSelection;
import jp.kusumotolab.kgenprog.ga.selection.VariantSelection;
import jp.kusumotolab.kgenprog.ga.validation.DefaultCodeValidation;
import jp.kusumotolab.kgenprog.ga.validation.SourceCodeValidation;
import jp.kusumotolab.kgenprog.output.PatchGenerator;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;

public class ClientLauncher {

  public static void main(final String[] args) {
    try {
      final Configuration config = Configuration.Builder.buildFromCmdLineArgs(args);
      final ClientLauncher launcher = new ClientLauncher();
      launcher.launch(config);
    } catch (final IllegalArgumentException e) {
      System.exit(1);
    }
  }

  public void launch(final Configuration config) {
    setLogLevel(config.getLogLevel());

    final FaultLocalization faultLocalization = new Ochiai();
    final Random random = new Random(config.getRandomSeed());
    final RouletteStatementSelection rouletteStatementSelection =
        new RouletteStatementSelection(random);
    final Mutation mutation = new RandomMutation(config.getMutationGeneratingCount(), random,
        rouletteStatementSelection, config.getScope());
    final Crossover crossover =
        new SinglePointCrossover(random, config.getCrossoverGeneratingCount());
    final SourceCodeGeneration sourceCodeGeneration = new DefaultSourceCodeGeneration();
    final SourceCodeValidation sourceCodeValidation = new DefaultCodeValidation();
    final VariantSelection variantSelection = new DefaultVariantSelection(config.getHeadcount());
    final TestExecutor testExecutor = new RemoteTestExecutor(config, "localhost", 50051);
    final PatchGenerator patchGenerator = new PatchGenerator();

    final KGenProgMain kGenProgMain =
        new KGenProgMain(config, faultLocalization, mutation, crossover, sourceCodeGeneration,
            sourceCodeValidation, variantSelection, testExecutor, patchGenerator);

    kGenProgMain.run();
  }

  private void setLogLevel(final Level logLevel) {
    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(logLevel);
  }
}
