package jp.kusumotolab.kgenprog.coordinator;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestValidator {

  private final Map<String, Instant> invalidateTimeMap = new ConcurrentHashMap<>();

  public void addInvalidateRequest(final ExecuteTestRequest request) {
    final Instant time = Instant.now();
    final String key = getKey(request.getSenderName(), request.getSenderPort());
    invalidateTimeMap.put(key, time);
  }

  public boolean validate(final ExecuteTestRequest request) {
    final String key = getKey(request.getSenderName(), request.getSenderPort());
    final Instant time = invalidateTimeMap.get(key);

    if (time == null) {
        return true;
    }

    // 失敗した後に来たリクエストは許可する
    return request.getTime()
        .isAfter(time);
  }

  private String getKey(final String host, final int port) {
    return host + ":" + port;
  }
}
