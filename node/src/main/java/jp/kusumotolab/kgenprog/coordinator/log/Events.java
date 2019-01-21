package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.JsonElement;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Events {

  Subject<EventBucket> eventSubject;
  ExecutorService executor;
  Observable<JsonElement> eventObservable;

  public Events() {
    eventSubject = PublishSubject.<EventBucket>create()
        .toSerialized();

    executor = Executors.newSingleThreadExecutor();
    eventObservable = eventSubject.subscribeOn(Schedulers.from(executor))
        .flatMap(EventBucket::consume);

    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::shutdown));
  }

  public void addEvent(Event event) {
    eventSubject.onNext(new EventBucket(Instant.now(), event));
  }

  public void shutdown() {
    executor.shutdown();
  }
  
  public Observable<JsonElement>  getEventObservable() {
    return eventObservable;
  }

  static class EventBucket {

    private final Instant time;
    private final Event event;

    public EventBucket(Instant time, Event event) {
      this.time = time;
      this.event = event;
    }

    private Observable<JsonElement> consume() {
      return event.consume(time);
    }
  }

}


interface Event {

  Observable<JsonElement> consume(Instant date);
}
