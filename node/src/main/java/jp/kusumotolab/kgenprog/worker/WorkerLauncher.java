package jp.kusumotolab.kgenprog.worker;

import jp.kusumotolab.kgenprog.worker.WorkerConfiguration.Builder;

public class WorkerLauncher {

  public static void main(final String[] args) {
    final WorkerLauncher workerLauncher = new WorkerLauncher();
    final WorkerConfiguration workerConfiguration = Builder.buildFromCmdLineArgs(args);
    workerLauncher.launch(workerConfiguration);
  }

  private void launch(final WorkerConfiguration configuration) {

  }
}
