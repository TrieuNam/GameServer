package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.knights.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnightsGrpcClient {

    @GrpcClient("knights-service")
    private KnightsServiceGrpc.KnightsServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public KnightsHandbookResponse getOrCreate(long roleId) {
        try {
            return stub.getOrCreate(GetOrCreateRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-knights] getOrCreate: knights-service unavailable");
            else log.error("[grpc-knights] getOrCreate error: {}", e.getMessage());
            return KnightsHandbookResponse.newBuilder().setSuccess(false).build();
        }
    }

    public KnightsHandbookResponse claimSeqReward(long roleId, int seqIndex) {
        try {
            return stub.claimSeqReward(ClaimSeqRewardRequest.newBuilder().setRoleId(roleId).setSeqIndex(seqIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-knights] claimSeqReward: knights-service unavailable");
            else log.error("[grpc-knights] claimSeqReward error: {}", e.getMessage());
            return KnightsHandbookResponse.newBuilder().setSuccess(false).build();
        }
    }

    public KnightsHandbookResponse claimLevelReward(long roleId, int levelIndex) {
        try {
            return stub.claimLevelReward(ClaimLevelRewardRequest.newBuilder().setRoleId(roleId).setLevelIndex(levelIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-knights] claimLevelReward: knights-service unavailable");
            else log.error("[grpc-knights] claimLevelReward error: {}", e.getMessage());
            return KnightsHandbookResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse getConditions(long roleId) {
        try {
            return stub.getConditions(GetConditionsRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-knights] getConditions: knights-service unavailable");
            else log.error("[grpc-knights] getConditions error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }
}
