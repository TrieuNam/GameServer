package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.pagoda.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PagodaGrpcClient {

    @GrpcClient("pagoda-service")
    private PagodaServiceGrpc.PagodaServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public ShiLianResponse getShiLian(long roleId) {
        try {
            return stub.getShiLian(GetShiLianRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] getShiLian: pagoda-service unavailable");
            else log.error("[grpc-pagoda] getShiLian error: {}", e.getMessage());
            return ShiLianResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ShiLianResponse challengeShiLian(long roleId, int p1) {
        try {
            return stub.challengeShiLian(ChallengeShiLianRequest.newBuilder().setRoleId(roleId).setP1(p1).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] challengeShiLian: pagoda-service unavailable");
            else log.error("[grpc-pagoda] challengeShiLian error: {}", e.getMessage());
            return ShiLianResponse.newBuilder().setSuccess(false).build();
        }
    }

    public BoolResponse claimShiLian(long roleId, int p1) {
        try {
            return stub.claimShiLian(ClaimShiLianRequest.newBuilder().setRoleId(roleId).setP1(p1).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] claimShiLian: pagoda-service unavailable");
            else log.error("[grpc-pagoda] claimShiLian error: {}", e.getMessage());
            return BoolResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse getGuMo(long roleId) {
        try {
            return stub.getGuMo(GetGuMoRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] getGuMo: pagoda-service unavailable");
            else log.error("[grpc-pagoda] getGuMo error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse challengeGuMo(long roleId, int layerId) {
        try {
            return stub.challengeGuMo(ChallengeGuMoRequest.newBuilder().setRoleId(roleId).setLayerId(layerId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] challengeGuMo: pagoda-service unavailable");
            else log.error("[grpc-pagoda] challengeGuMo error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public BoolResponse claimGuMo(long roleId, int layerId) {
        try {
            return stub.claimGuMo(ClaimGuMoRequest.newBuilder().setRoleId(roleId).setLayerId(layerId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-pagoda] claimGuMo: pagoda-service unavailable");
            else log.error("[grpc-pagoda] claimGuMo error: {}", e.getMessage());
            return BoolResponse.newBuilder().setSuccess(false).build();
        }
    }
}
