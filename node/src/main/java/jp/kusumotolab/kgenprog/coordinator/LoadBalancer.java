package jp.kusumotolab.kgenprog.coordinator;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class LoadBalancer {

  private final ConcurrentMap<Worker, Worker> workerMap = new ConcurrentHashMap<>();
  private final Subject<Worker> workerSubject;

  public LoadBalancer() {
    final BehaviorSubject<Worker> behaviorSubject = BehaviorSubject.create();
    workerSubject = behaviorSubject.toSerialized();
  }

  public void addWorker(final Worker worker) {
    workerMap.putIfAbsent(worker, worker);
    workerSubject.onNext(worker);
  }

  public void finish(final Worker worker) {
    workerSubject.onNext(worker);
  }

  public Collection<Worker> getWorkerCollection() {
    return workerMap.values();
  }

  public Observable<Worker> getHotWorkerObserver() {
    return workerSubject;
  }
}
