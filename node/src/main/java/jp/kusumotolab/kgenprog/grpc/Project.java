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
import jp.kusumotolab.kgenprog.ga.variant.Variant;
import jp.kusumotolab.kgenprog.ga.variant.VariantStore;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.test.LocalTestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestExecutor;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class Project {

  private final Configuration config;
  private final int projectId;
  private final Strategies strategies;
  private final VariantStore variantStore;

  public Project(final int projectId, final Configuration config) {
    this.config = config;
    this.projectId = projectId;

    this.strategies = createStrategies(this.config);
    this.variantStore = new VariantStore(this.config, strategies);
  }

  public TestResults executeTest(final Gene gene) {
    final Variant variant = variantStore.createVariant(gene, EmptyHistoricalElement.shared);
    return variant.getTestResults();
  }

  public void unregister() {

  }

  public int getProjectId() {
    return projectId;
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
