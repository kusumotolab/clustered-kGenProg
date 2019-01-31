package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;

@SuppressWarnings("unused")
class TestRequestQueue implements EventDocument {

  private final Instant date;
  private final int coordinatorValue;
  private final int workerId;
  private final int value;

  public TestRequestQueue(final Instant date, final int coordinatorValue, final int workerId,
      final int value) {
    this.date = date;
    this.coordinatorValue = coordinatorValue;
    this.workerId = workerId;
    this.value = value;
  }

  @Override
  public String getType() {
    return "testRequestQueue";
  }
}
