package com.SouthMillion.webSocket_server.service;

import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Global Client Interceptor — inject Authorization header vào mọi gRPC call.
 *
 * <p>Hoạt động song song với {@link FeignAuthInterceptor}: cả hai đọc token từ
 * {@link FeignTokenHolder} (ThreadLocal), đảm bảo mọi outbound call (REST + gRPC)
 * đều có {@code Authorization: Bearer <token>}.
 *
 * <p>Token được set bởi:
 * <ul>
 *   <li>{@code WsGatewayHandler.dispatch()} — trước khi xử lý mỗi client message</li>
 *   <li>{@code LoginBootstrapHandler.safe()} — trước khi chạy mỗi pushAll() service</li>
 * </ul>
 *
 * <p>{@code @GrpcGlobalClientInterceptor} tự động áp dụng cho <strong>mọi</strong>
 * {@code @GrpcClient} bean trong application — không cần sửa từng gRPC client.
 */
@GrpcGlobalClientInterceptor
public class GrpcAuthClientInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcAuthClientInterceptor.class);

    /** Key gRPC metadata cho Authorization header (lowercase per HTTP/2 spec) */
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = FeignTokenHolder.get();
                if (token != null && !token.isBlank()) {
                    // Tránh double "Bearer Bearer ..."
                    String auth = token.startsWith("Bearer ") ? token : "Bearer " + token;
                    headers.put(AUTHORIZATION_KEY, auth);
                    log.debug("[grpc-auth] add Authorization (len={}) for {}",
                            token.length(), method.getFullMethodName());
                } else {
                    log.debug("[grpc-auth] NO token for {}", method.getFullMethodName());
                }
                super.start(responseListener, headers);
            }
        };
    }
}

