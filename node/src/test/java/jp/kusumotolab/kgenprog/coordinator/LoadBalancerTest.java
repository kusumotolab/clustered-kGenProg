package jp.kusumotolab.kgenprog.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.Test;
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

    final Worker worker4 = loadBalancer.getWorker();
    final Worker worker5 = loadBalancer.getWorker();
    final Worker worker6 = loadBalancer.getWorker();

    // 全て異なるはず
    assertThat(worker4).isNotEqualTo(worker5).isNotEqualTo(worker6);
    assertThat(worker5).isNotEqualTo(worker6);

    // worker4だけ先に終わるとする
    loadBalancer.finish(worker4);

    // worker4の負荷が減ったはずなので、次に取得するworkerは4のはず
    final Worker worker7 = loadBalancer.getWorker();
    assertThat(worker7).isEqualTo(worker4);
  }
}