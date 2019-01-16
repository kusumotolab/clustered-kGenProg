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

public class ClientHostNameCaptor implements ServerInterceptor {

  private final Context.Key<String> hostNameKey = Context.key("CLIENT_HOST_NAME");

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
      final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {

    final String hostName;
    final SocketAddress socketAddress = call.getAttributes()
        .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

    if (socketAddress instanceof InetSocketAddress) {
      final InetSocketAddress client = (InetSocketAddress) socketAddress;
      hostName = client.getHostName();
    } else {
      hostName = "localhost";
    }

    Context context = Context.current()
        .withValue(hostNameKey, hostName);
    return Contexts.interceptCall(context, call, headers, next);
  }

  public String getHostName() {
    return hostNameKey.get();
  }

}
