package jp.kusumotolab.kgenprog.coordinator;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessSocketAddress;

public class ClientHostAddressCaptor implements ServerInterceptor {

  private final Context.Key<String> hostNameKey = Context.key("CLIENT_HOST_NAME");
  private final Context.Key<Integer> portKey = Context.key("CLIENT_PORT");

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
      final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {

    final String hostName;
    final int port;
    final SocketAddress socketAddress = call.getAttributes()
        .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

    if (socketAddress instanceof InetSocketAddress) {
      final InetSocketAddress client = (InetSocketAddress) socketAddress;
      hostName = client.getAddress().getHostAddress();
      port = client.getPort();
    } else if (socketAddress instanceof InProcessSocketAddress) {
      // テストのとき
      final InProcessSocketAddress client = (InProcessSocketAddress)socketAddress;
      hostName = client.getName();
      port = -1;
    } else {
      throw new RuntimeException();
    }

    final Context context = Context.current()
        .withValue(hostNameKey, hostName)
        .withValue(portKey, port);
    return Contexts.interceptCall(context, call, headers, next);
  }

  public String getHostName() {
    return hostNameKey.get();
  }

  public int getPort() {
    return portKey.get();
  }
}
