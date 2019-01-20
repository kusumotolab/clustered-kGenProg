package jp.kusumotolab.kgenprog.coordinator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import io.grpc.stub.StreamObserver;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcExecuteTestResponse;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectRequest;
import jp.kusumotolab.kgenprog.grpc.GrpcUnregisterProjectResponse;
import jp.kusumotolab.kgenprog.grpc.Worker;

public class WorkerSetTest {

  @Test
  public void testGetWorkerList() {
    final Worker worker1 = mock(Worker.class);
    final Worker worker2 = mock(Worker.class);
    final Worker worker3 = mock(Worker.class);

    final Subject<GrpcExecuteTestResponse> responseSubject1 = PublishSubject.create();
    final Subject<GrpcExecuteTestResponse> responseSubject2 = PublishSubject.create();
    final Subject<GrpcExecuteTestResponse> responseSubject3 = PublishSubject.create();

    // SubjectXがonNext()されるとWorkerXのSingleがonNext()される
    when(worker1.executeTest(any())).then(
        invocation -> Single.create(emitter -> responseSubject1.subscribe(emitter::onSuccess)));
    when(worker2.executeTest(any())).then(
        invocation -> Single.create(emitter -> responseSubject2.subscribe(emitter::onSuccess)));
    when(worker3.executeTest(any())).then(
        invocation -> Single.create(emitter -> responseSubject3.subscribe(emitter::onSuccess)));

    final WorkerSet workerSet = new WorkerSet();
    workerSet.addWorker(worker1);
    workerSet.addWorker(worker2);
    workerSet.addWorker(worker3);

    final GrpcExecuteTestRequest request = GrpcExecuteTestRequest.newBuilder()
        .build();
    @SuppressWarnings("unchecked") final StreamObserver<GrpcExecuteTestResponse> mockObserver = mock(
        StreamObserver.class);
    final ExecuteTestRequest testRequest = new ExecuteTestRequest(request, mockObserver,
        "localhost", 8080);
    final GrpcExecuteTestResponse response = GrpcExecuteTestResponse.newBuilder()
        .build();

    // addWork()した順に呼ばれるはず
    workerSet.executeTest(testRequest);
    verify(worker1, times(1)).executeTest(any());
    verify(worker2, times(0)).executeTest(any());
    verify(worker3, times(0)).executeTest(any());
    verify(mockObserver, times(0)).onNext(any());

    workerSet.executeTest(testRequest);
    verify(worker1, times(1)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(0)).executeTest(any());

    workerSet.executeTest(testRequest);
    verify(worker1, times(1)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(1)).executeTest(any());

    // responseSubjectX.onNext()するたびobserver.onNext()が呼ばれる
    responseSubject1.onNext(response);
    verify(mockObserver, times(1)).onNext(any());

    responseSubject3.onNext(response);
    verify(mockObserver, times(2)).onNext(any());

    // responseSubjectX.onNext()した順番でworkerX.executeTest()が呼ばれる
    workerSet.executeTest(testRequest);
    verify(worker1, times(2)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(1)).executeTest(any());

    workerSet.executeTest(testRequest);
    verify(worker1, times(2)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(2)).executeTest(any());

    // responseSubject3.onNext()していないので、workerSet.executeTestしても
    // workerX.executeTest()が呼ばれない
    workerSet.executeTest(testRequest);
    verify(worker1, times(2)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(2)).executeTest(any());

    // worker2が終わっていなくても、worker3が先に終われば、worker2のexecuteTest()が呼ばれる
    responseSubject3.onNext(response);
    // workerSet.executeTest(testRequest); <- 上でしているので不要
    verify(mockObserver, times(3)).onNext(any());
    verify(worker1, times(2)).executeTest(any());
    verify(worker2, times(1)).executeTest(any());
    verify(worker3, times(3)).executeTest(any());

    responseSubject2.onNext(response);
    workerSet.executeTest(testRequest);
    verify(mockObserver, times(4)).onNext(any());
    verify(worker1, times(2)).executeTest(any());
    verify(worker2, times(2)).executeTest(any());
    verify(worker3, times(3)).executeTest(any());
  }

  @Test
  public void testUnregister() {
    final Worker worker1 = mock(Worker.class);
    final Worker worker2 = mock(Worker.class);
    final Worker worker3 = mock(Worker.class);

    final GrpcUnregisterProjectResponse response = GrpcUnregisterProjectResponse.newBuilder()
        .build();

    when(worker1.unregisterProject(any())).thenReturn(Single.just(response));
    when(worker2.unregisterProject(any())).thenReturn(Single.just(response));
    when(worker3.unregisterProject(any())).thenReturn(Single.just(response));

    final WorkerSet workerSet = new WorkerSet();
    workerSet.addWorker(worker1);
    workerSet.addWorker(worker2);
    workerSet.addWorker(worker3);

    workerSet.unregister(GrpcUnregisterProjectRequest.newBuilder()
        .build());
    verify(worker1, times(1)).unregisterProject(any());
    verify(worker2, times(1)).unregisterProject(any());
    verify(worker3, times(1)).unregisterProject(any());
  }
}