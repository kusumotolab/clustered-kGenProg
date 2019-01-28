package jp.kusumotolab.kgenprog.project.test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import jp.kusumotolab.kgenprog.project.FullyQualifiedName;
import jp.kusumotolab.kgenprog.project.TargetFullyQualifiedName;
import jp.kusumotolab.kgenprog.project.test.Coverage.Status;

public class CompressedCoverageTest {

  @Test
  public void test() {
    final FullyQualifiedName fqn = new TargetFullyQualifiedName("foo");
    final CompressedCoverage coverage = new CompressedCoverage(fqn, 10);
    assertThat(coverage.getStatus(0)).isSameAs(Status.NOT_COVERED);
    assertThat(coverage.getStatusesSize()).isEqualTo(10);
  }
}