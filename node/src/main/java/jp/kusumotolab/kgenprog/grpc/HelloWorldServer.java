package jp.kusumotolab.kgenprog.grpc;

import java.io.IOException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class HelloWorldServer extends GreeterGrpc.GreeterImplBase {

  public static void main(final String[] args) throws IOException, InterruptedException {
    final int port = 50051;
    final Server server = ServerBuilder.forPort(port)
        .addService(new HelloWorldServer())
        .build()
        .start();

    server.awaitTermination();

    Runtime.getRuntime()
        .addShutdownHook(new Thread() {

          @Override
          public void run() {
            if (server != null) {
              server.shutdown();
            }
          }
        });
  }

  @Override
  public void sayHello(final HelloRequest request,
      final StreamObserver<HelloReply> responseObserver) {
    System.out.println("Request from " + request.getName());

    responseObserver.onNext(HelloReply.newBuilder()
        .setMessage("Hello! " + request.getName())
        .build());
    responseObserver.onCompleted();
  }

}
