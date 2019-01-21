package jp.kusumotolab.kgenprog.coordinator.log;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Events {

  Subject<Event> eventSubject;
  ExecutorService executor;


  public Events() {
    eventSubject = PublishSubject.<Event>create()
        .toSerialized();

    eventSubject.subscribe(this::subscribeEvent);
    executor = Executors.newSingleThreadExecutor();

    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::shutdown));
  }

  public void addEvent(Event event) {
    eventSubject.onNext(event);
  }
  
  public void shutdown() {
    executor.shutdown();
  }

  private void subscribeEvent(Event event) {
    event.accept(this);
  }
  
  void visit(RequestStart event) {
    
  }
  
  void visit(RequestFinish event) {
    
  }

}

class RequestStart implements Event {

  @Override
  public void accept(Events events) {
    events.visit(this);
  }
  
}

class RequestFinish implements Event {

  @Override
  public void accept(Events events) {
    events.visit(this);
  }
  
}

interface Event {
  void accept(Events events);
}
