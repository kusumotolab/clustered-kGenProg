package jp.kusumotolab.kgenprog.grpc;

import java.util.Collections;
import java.util.List;
import jp.kusumotolab.kgenprog.ga.variant.HistoricalElement;
import jp.kusumotolab.kgenprog.ga.variant.Variant;

public class EmptyHistoricalElement implements HistoricalElement {

  static final EmptyHistoricalElement shared = new EmptyHistoricalElement();

  private EmptyHistoricalElement() {
  }

  @Override
  public List<Variant> getParents() {
    return Collections.emptyList();
  }

  @Override
  public String getOperationName() {
    return "None";
  }
}
