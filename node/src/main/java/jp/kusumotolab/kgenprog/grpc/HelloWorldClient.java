package jp.kusumotolab.kgenprog.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jp.kusumotolab.kgenprog.grpc.GreeterGrpc.GreeterBlockingStub;

public class HelloWorldClient {

  public static void main(final String[] args) {
    final ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
        .usePlaintext()
        .build();
    final GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(managedChannel);


    final HelloRequest request = HelloRequest.newBuilder()
        .setName(args[0])
        .build();

    final HelloReply response = blockingStub.sayHello(request);
    System.out.println(response.getMessage());
  }

}
