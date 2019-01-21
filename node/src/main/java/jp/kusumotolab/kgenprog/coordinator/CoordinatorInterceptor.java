package jp.kusumotolab.kgenprog.coordinator;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.inprocess.InProcessSocketAddress;
import jp.kusumotolab.kgenprog.coordinator.log.CoordinatorLogger;

public class CoordinatorInterceptor implements ServerInterceptor {

  private final Context.Key<String> hostNameKey = Context.key("CLIENT_HOST_NAME");
  private final Context.Key<Integer> requestIdKey = Context.key("REQUEST_ID");
  private final AtomicInteger requestIdCounter = new AtomicInteger(0);

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
      final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {
    final int requestId = requestIdCounter.getAndIncrement();
    final String hostName = captureHostName(call);
    final Context context = Context.current()
        .withValue(requestIdKey, requestId)
        .withValue(hostNameKey, hostName);

    return Contexts.interceptCall(context, new ServerCallWrapper<>(call, requestId, hostName), headers, next);
  }

  private <ReqT, RespT> String captureHostName(final ServerCall<ReqT, RespT> call) {
    final String hostName;
    final SocketAddress socketAddress = call.getAttributes()
        .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

    if (socketAddress instanceof InetSocketAddress) {
      final InetSocketAddress client = (InetSocketAddress) socketAddress;
      hostName = client.getAddress()
          .getHostAddress();
    } else if (socketAddress instanceof InProcessSocketAddress) {
      // テストのとき
      final InProcessSocketAddress client = (InProcessSocketAddress) socketAddress;
      hostName = client.getName();
    } else {
      throw new RuntimeException();
    }

    return hostName;
  }

  public String getHostName() {
    return hostNameKey.get();
  }

  public Integer getRequestId() {
    return requestIdKey.get();
  }

  private static class ServerCallWrapper<ReqT, RespT>
      extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

    private final int requestId;
    private final String clientIp;

    protected ServerCallWrapper(ServerCall<ReqT, RespT> delegate, final int requestId, final String clientIp) {
      super(delegate);
      this.requestId = requestId;
      this.clientIp = clientIp;
    }

    @Override
    public void request(final int numMessages) {
      CoordinatorLogger.request(requestId, clientIp);
      super.request(numMessages);
    }

    @Override
    public void close(final Status status, final Metadata trailers) {
      CoordinatorLogger.close(requestId, clientIp);
      super.close(status, trailers);
    }
  }
}
