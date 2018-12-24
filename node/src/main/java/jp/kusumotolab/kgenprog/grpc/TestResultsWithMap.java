package jp.kusumotolab.kgenprog.grpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jp.kusumotolab.kgenprog.project.FullyQualifiedName;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.test.TestResults;

public class TestResultsWithMap extends TestResults {

  private static final long serialVersionUID = 1L;

  private Map<ProductSourcePath, Set<FullyQualifiedName>> map = new HashMap<>();


  public void setSourcePathToFQN(final Map<ProductSourcePath, Set<FullyQualifiedName>> map) {
    this.map = map;
  }

  @Override
  public Set<FullyQualifiedName> getCorrespondingFqns(final ProductSourcePath productSourcePath) {
    return map.get(productSourcePath);
  }
}
