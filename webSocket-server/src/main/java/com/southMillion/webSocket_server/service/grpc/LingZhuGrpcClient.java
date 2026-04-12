package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.lingzhu.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LingZhuGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LingZhuGrpcClient.class);

    @GrpcClient("lingzhu-service")
    private LingZhuServiceGrpc.LingZhuServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetAllResponse getAll(long roleId) {
        try {
            return stub.getAll(GetAllRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-lingzhu] getAll: lingzhu-service unavailable");
            else log.error("[grpc-lingzhu] getAll error: {}", e.getMessage());
            return GetAllResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse challenge(long roleId, int stage, int p1) {
        try {
            return stub.challenge(ChallengeRequest.newBuilder().setRoleId(roleId).setStage(stage).setP1(p1).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-lingzhu] challenge: lingzhu-service unavailable");
            else log.error("[grpc-lingzhu] challenge error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse sweep(long roleId, int stage, int count) {
        try {
            return stub.sweep(SweepRequest.newBuilder().setRoleId(roleId).setStage(stage).setCount(count).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-lingzhu] sweep: lingzhu-service unavailable");
            else log.error("[grpc-lingzhu] sweep error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GenericResponse finishChallenge(long roleId, int stage, int level) {
        try {
            return stub.finishChallenge(FinishChallengeRequest.newBuilder().setRoleId(roleId).setStage(stage).setLevel(level).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-lingzhu] finishChallenge: lingzhu-service unavailable");
            else log.error("[grpc-lingzhu] finishChallenge error: {}", e.getMessage());
            return GenericResponse.newBuilder().setSuccess(false).build();
        }
    }
}
