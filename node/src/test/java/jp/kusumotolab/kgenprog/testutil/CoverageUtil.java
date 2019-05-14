package jp.kusumotolab.kgenprog.testutil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jp.kusumotolab.kgenprog.project.test.Coverage;
import jp.kusumotolab.kgenprog.project.test.Coverage.Status;

public class CoverageUtil {

  public static List<Status> extractStatuses(final Coverage coverage) {
    return IntStream.range(0, coverage.getStatusesSize())
        .mapToObj(coverage::getStatus)
        .collect(Collectors.toList());
  }
}
