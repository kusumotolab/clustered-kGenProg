package jp.kusumotolab.kgenprog.coordinator.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Events {

  private static final Logger log = LoggerFactory.getLogger(Events.class);
  private static final Gson gson = createGson();
  public static final Events events = new Events();

  static {
    events.eventObservable.subscribe(doc -> {
      final JsonObject object = new JsonObject();
      object.addProperty("type", doc.getType());
      object.add(doc.getType(), gson.toJsonTree(doc));
      log.info(gson.toJson(object));
    });
  }

  Subject<EventBucket> eventSubject;
  ExecutorService executor;
  Observable<EventDocument> eventObservable;

  private Events() {
    eventSubject = PublishSubject.<EventBucket>create()
        .toSerialized();

    executor = Executors.newSingleThreadExecutor();
    eventObservable = eventSubject.subscribeOn(Schedulers.from(executor))
        .flatMap(EventBucket::consume);

    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::shutdown));
  }

  public void addEvent(final Event event) {
    eventSubject.onNext(new EventBucket(Instant.now(), event));
  }

  public void shutdown() {
    executor.shutdown();
  }

  public Observable<EventDocument> getEventObservable() {
    return eventObservable;
  }

  static class EventBucket {

    private final Instant time;
    private final Event event;

    public EventBucket(final Instant time, final Event event) {
      this.time = time;
      this.event = event;
    }

    private Observable<EventDocument> consume() {
      return event.consume(time);
    }
  }

  private static Gson createGson() {
    final GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Instant.class,
        (JsonSerializer<Instant>) ((src, typeOfSrc, context) -> {
          final ZonedDateTime zonedDateTime = src.atZone(ZoneId.systemDefault());
          return new JsonPrimitive(zonedDateTime.toString());
        }));

    return gsonBuilder.create();
  }
}
