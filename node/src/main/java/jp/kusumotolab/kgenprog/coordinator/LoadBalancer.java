package jp.kusumotolab.kgenprog.coordinator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class LoadBalancer {

  final List<Worker> allWorkerList = new ArrayList<>();
  final Map<Worker, Integer> taskMap = new HashMap<>();

  public void addWorker(final Worker worker) {
    allWorkerList.add(worker);
    taskMap.put(worker, 0);
  }

  public Worker getWorker() {
    if (taskMap.isEmpty()) {
      throw new RuntimeException("LoadBalancer has no worker.");
    }

    final Entry<Worker, Integer> minEntry = taskMap.entrySet()
        .stream()
        .min(Comparator.comparingInt(Entry::getValue))
        .get();

    final Worker worker = minEntry.getKey();
    final Integer task = minEntry.getValue();

    taskMap.put(worker, task + 1);
    return worker;
  }

  public void finish(final Worker worker) {
    final Integer task = taskMap.get(worker);
    taskMap.put(worker, task - 1);
  }

  public List<Worker> getWorkerList() {
    return allWorkerList;
  }
}
