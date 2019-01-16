package jp.kusumotolab.kgenprog.coordinator;

import java.net.InetSocketAddress;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class ClientHostnameCaptor implements ServerInterceptor {

  private final Context.Key<String> hostnameKey = Context.key("CLIENT_HOST_NAME");

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
      final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {
    final InetSocketAddress client = (InetSocketAddress) call.getAttributes()
        .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
    final Context context = Context.current()
        .withValue(hostnameKey, client.getHostName());
    return Contexts.interceptCall(context, call, headers, next);
  }

  public String getHostName() {
    return hostnameKey.get();
  }

}
