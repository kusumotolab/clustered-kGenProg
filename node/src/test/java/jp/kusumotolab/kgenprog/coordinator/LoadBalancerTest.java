package jp.kusumotolab.kgenprog.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import io.reactivex.disposables.Disposable;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class LoadBalancerTest {

  @Test
  public void testGetWorkerList() {
    final Worker worker1 = mock(Worker.class);
    final Worker worker2 = mock(Worker.class);
    final Worker worker3 = mock(Worker.class);

    final LoadBalancer loadBalancer = new LoadBalancer();
    loadBalancer.addWorker(worker1);
    loadBalancer.addWorker(worker2);
    loadBalancer.addWorker(worker3);

    assertThat(loadBalancer.getWorkerList()).hasSize(3);
  }

  @Test
  public void testGetWorker() {
    final Worker worker1 = mock(Worker.class);
    final Worker worker2 = mock(Worker.class);
    final Worker worker3 = mock(Worker.class);

    final LoadBalancer loadBalancer = new LoadBalancer();

    loadBalancer.addWorker(worker1);
    loadBalancer.addWorker(worker2);
    loadBalancer.addWorker(worker3);

    final AtomicReference<Worker> worker = new AtomicReference<>();
    final Disposable subscribe = loadBalancer.getHotWorkerObserver()
        .subscribe(worker::set);

    loadBalancer.finish(worker1);
    assertThat(worker.get()).isEqualTo(worker1);

    loadBalancer.finish(worker2);
    assertThat(worker.get()).isEqualTo(worker2);

    loadBalancer.finish(worker1);
    assertThat(worker.get()).isEqualTo(worker1);

    loadBalancer.finish(worker3);
    assertThat(worker.get()).isEqualTo(worker3);

    subscribe.dispose();
  }
}