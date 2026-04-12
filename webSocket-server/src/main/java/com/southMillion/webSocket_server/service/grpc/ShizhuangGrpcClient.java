package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.shizhuang.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShizhuangGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShizhuangGrpcClient.class);

    @GrpcClient("shizhuang-service")
    private ShizhuangServiceGrpc.ShizhuangServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public ShizhuangListResponse listByRole(long roleId) {
        try {
            return stub.listByRole(RoleRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-shizhuang] listByRole: shizhuang-service unavailable");
            else log.error("[grpc-shizhuang] listByRole error: {}", e.getMessage());
            return ShizhuangListResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ShizhuangInfoResponse getInfo(long roleId, int shizhuangId) {
        try {
            return stub.getInfo(GetInfoRequest.newBuilder().setRoleId(roleId).setShizhuangId(shizhuangId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-shizhuang] getInfo: shizhuang-service unavailable");
            else log.error("[grpc-shizhuang] getInfo error: {}", e.getMessage());
            return ShizhuangInfoResponse.newBuilder().setSuccess(false).build();
        }
    }

    public OperationResponse activate(long roleId, int shizhuangId) {
        try {
            return stub.activate(ActivateRequest.newBuilder().setRoleId(roleId).setShizhuangId(shizhuangId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-shizhuang] activate: shizhuang-service unavailable");
            else log.error("[grpc-shizhuang] activate error: {}", e.getMessage());
            return OperationResponse.newBuilder().setSuccess(false).build();
        }
    }

    public OperationResponse wear(long roleId, int shizhuangId) {
        try {
            return stub.wear(WearRequest.newBuilder().setRoleId(roleId).setShizhuangId(shizhuangId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-shizhuang] wear: shizhuang-service unavailable");
            else log.error("[grpc-shizhuang] wear error: {}", e.getMessage());
            return OperationResponse.newBuilder().setSuccess(false).build();
        }
    }

    public OperationResponse levelUp(long roleId, int shizhuangId) {
        try {
            return stub.levelUp(LevelUpRequest.newBuilder().setRoleId(roleId).setShizhuangId(shizhuangId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-shizhuang] levelUp: shizhuang-service unavailable");
            else log.error("[grpc-shizhuang] levelUp error: {}", e.getMessage());
            return OperationResponse.newBuilder().setSuccess(false).build();
        }
    }
}
