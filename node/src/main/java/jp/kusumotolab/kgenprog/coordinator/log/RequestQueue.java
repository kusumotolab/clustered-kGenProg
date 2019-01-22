package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;

@SuppressWarnings("unused") class RequestQueue implements EventDocument {

  private final Instant date;
  private final String method;
  private final int value;

  public RequestQueue(Instant date, String method, int value) {
    this.date = date;
    this.method = method;
    this.value = value;
  }

  @Override
  public String getType() {
    return "requestQueue";
  }
}