package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.gem.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class GemGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GemGrpcClient.class);

    @GrpcClient("gem-service")
    private GemServiceGrpc.GemServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GemInfoResponse getGemInfo(long roleId) {
        try {
            return stub.getGemInfo(GetGemInfoRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] getGemInfo: gem-service unavailable");
            else log.error("[grpc-gem] getGemInfo error: {}", e.getMessage());
            return GemInfoResponse.newBuilder().setSuccess(false).setDrawingId(-1).build();
        }
    }

    public GenericResponse inlay(long roleId, int gemId, int slotType) {
        try {
            return stub.inlay(InlayRequest.newBuilder().setRoleId(roleId).setGemId(gemId).setSlotType(slotType).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] inlay: gem-service unavailable");
            else log.error("[grpc-gem] inlay error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build();
        }
    }

    public GenericResponse remove(long roleId, int gemId) {
        try {
            return stub.remove(RemoveRequest.newBuilder().setRoleId(roleId).setGemId(gemId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] remove: gem-service unavailable");
            else log.error("[grpc-gem] remove error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse compose(long roleId, List<Integer> sourceGemIds) {
        try {
            return stub.compose(ComposeRequest.newBuilder().setRoleId(roleId).addAllSourceGemIds(sourceGemIds).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] compose: gem-service unavailable");
            else log.error("[grpc-gem] compose error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse upgradeAll(long roleId, List<Integer> gemIds) {
        try {
            return stub.upgradeAll(UpgradeAllRequest.newBuilder().setRoleId(roleId).addAllGemIds(gemIds).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] upgradeAll: gem-service unavailable");
            else log.error("[grpc-gem] upgradeAll error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse buy(long roleId, List<Integer> gemIds) {
        try {
            return stub.buy(BuyRequest.newBuilder().setRoleId(roleId).addAllGemIds(gemIds).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-gem] buy: gem-service unavailable");
            else log.error("[grpc-gem] buy error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }
}
