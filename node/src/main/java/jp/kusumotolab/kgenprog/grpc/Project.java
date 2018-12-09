package jp.kusumotolab.kgenprog.grpc;

import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.Strategies;
import jp.kusumotolab.kgenprog.fl.FaultLocalization;
import jp.kusumotolab.kgenprog.fl.Ochiai;
import jp.kusumotolab.kgenprog.ga.codegeneration.DefaultSourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.codegeneration.SourceCodeGeneration;
import jp.kusumotolab.kgenprog.ga.selection.GenerationalVariantSelection;
import jp.kusumotolab.kgenprog.ga.selection.VariantSelection;
import jp.kusumotolab.kgenprog.ga.validation.DefaultCodeValidation;
import jp.kusumotolab.kgenprog.ga.validation.SourceCodeValidation;
import jp.kusumotolab.kgenprog.ga.variant.Gene;
import jp.kusumotolab.kgenprog.ga.variant.VariantStore;
import jp.kusumotolab.kgenprog.project.GeneratedSourceCode;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.test.LocalTestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class Project {

  private final Configuration config;
  private final Strategies strategies;
  private final VariantStore variantStore;

  public Project(final int projectId, final Configuration config) {
    this.config = config;

    this.strategies = createStrategies(this.config);
    this.variantStore = new VariantStore(this.config, strategies);
  }

  public TestResults executeTest(final Gene gene) {
    final GeneratedSourceCode sourceCode = strategies.execSourceCodeGeneration(variantStore, gene);
    final TestResults results = strategies.execTestExecutor(sourceCode);
    return results;
  }

  public void unregister() {

  }

  private Strategies createStrategies(final Configuration configuration) {
    final FaultLocalization faultLocaliztion = new Ochiai();
    final JDTASTConstruction astConstruction = new JDTASTConstruction();
    final SourceCodeGeneration sourceCodeGeneration = new DefaultSourceCodeGeneration();
    final SourceCodeValidation sourceCodeValidation = new DefaultCodeValidation();
    final TestExecutor testExecutor = new LocalTestExecutor(configuration);
    final VariantSelection variantSelection = new GenerationalVariantSelection();
    return new Strategies(faultLocaliztion, astConstruction, sourceCodeGeneration,
        sourceCodeValidation, testExecutor, variantSelection);
  }
}
