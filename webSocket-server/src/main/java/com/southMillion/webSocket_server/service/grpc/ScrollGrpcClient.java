package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.scroll.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScrollGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScrollGrpcClient.class);

    @GrpcClient("scroll-service")
    private ScrollServiceGrpc.ScrollServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public ScrollMetaResponse getMeta(long roleId) {
        try {
            return stub.getMeta(GetMetaRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-scroll] getMeta: scroll-service unavailable");
            else log.error("[grpc-scroll] getMeta error: {}", e.getMessage());
            return ScrollMetaResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GetListResponse getList(long roleId) {
        try {
            return stub.getList(GetListRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-scroll] getList: scroll-service unavailable");
            else log.error("[grpc-scroll] getList error: {}", e.getMessage());
            return GetListResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse draw(long roleId, int count) {
        try {
            return stub.draw(DrawRequest.newBuilder().setRoleId(roleId).setCount(count).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-scroll] draw: scroll-service unavailable");
            else log.error("[grpc-scroll] draw error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }
}
