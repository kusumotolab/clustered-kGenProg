package jp.kusumotolab.kgenprog.coordinator;

import java.util.ArrayList;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class LoadBalancer {

  private final List<Worker> allWorkerList = new ArrayList<>();
  private final Subject<Worker> workerSubject = BehaviorSubject.create();

  public void addWorker(final Worker worker) {
    allWorkerList.add(worker);
    workerSubject.onNext(worker);
  }

  public synchronized void finish(final Worker worker) {
    workerSubject.onNext(worker);
  }

  public List<Worker> getWorkerList() {
    return allWorkerList;
  }

  public Observable<Worker> getHotWorkerObserver() {
    return workerSubject;
  }
}
