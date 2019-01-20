package jp.kusumotolab.kgenprog.coordinator;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestValidator {

  private final Map<String, InvalidateInformation> invalidateInformationMap = new ConcurrentHashMap<>();

  public void addInvalidateRequest(final ExecuteTestRequest request) {
    final InvalidateInformation information = new InvalidateInformation(request.getSenderPort(),
        request.getDate());
    invalidateInformationMap.put(request.getSenderName(), information);
  }

  public boolean validate(final ExecuteTestRequest request) {
    final InvalidateInformation information = invalidateInformationMap.get(
        request.getSenderName());
    if (information == null) {
      return true;
    }
    if (information.port != request.getSenderPort()) {
      return true;
    }
    if (request.getDate()
        .before(information.date)) {
      return true;
    }
    return false;
  }

  private class InvalidateInformation {

    private final int port;
    private final Date date;

    public InvalidateInformation(final int port, final Date date) {
      this.port = port;
      this.date = date;
    }
  }
}
