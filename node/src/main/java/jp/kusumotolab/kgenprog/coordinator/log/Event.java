package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;
import io.reactivex.Observable;

public interface Event {

  Observable<EventDocument> consume(Instant date);
}
