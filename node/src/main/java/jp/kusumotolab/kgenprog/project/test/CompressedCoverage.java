package jp.kusumotolab.kgenprog.project.test;

import org.apache.commons.lang3.StringUtils;
import jp.kusumotolab.kgenprog.project.FullyQualifiedName;

public class CompressedCoverage implements Coverage {

  private final FullyQualifiedName executedTargetFQN;
  private final int size;

  public CompressedCoverage(final FullyQualifiedName executedTargetFQN, final int size) {
    this.executedTargetFQN = executedTargetFQN;
    this.size = size;
  }

  @Override
  public FullyQualifiedName getExecutedTargetFQN() {
    return executedTargetFQN;
  }

  @Override
  public Status getStatus(final int index) {
    return Status.NOT_COVERED;
  }

  @Override
  public int getStatusesSize() {
    return size;
  }

  @Override
  public String toString() {
    return toString(0);
  }

  @Override
  public String toString(final int indentDepth) {
    final StringBuilder sb = new StringBuilder();
    final String indent = StringUtils.repeat(" ", indentDepth);
    sb.append(indent + "  {");
    sb.append("\"message\": \"this coverage is compressed.\"");
    sb.append("\"executedTargetFQN\": \"" + executedTargetFQN + "\", ");
    sb.append("\"coverages\": [");
    sb.append("]}");
    return sb.toString();
  }
}
