package com.SouthMillion.knights_service.grpc;

import com.SouthMillion.knights_service.entity.KnightsHandbook;
import com.SouthMillion.knights_service.service.KnightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.knights.*;

import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class KnightsServiceGrpcImpl extends KnightsServiceGrpc.KnightsServiceImplBase {

    private final KnightsService knightsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void getOrCreate(GetOrCreateRequest request, StreamObserver<KnightsHandbookResponse> observer) {
        log.info("[KnightsGrpc] GetOrCreate: roleId={}", request.getRoleId());
        try {
            KnightsHandbook h = knightsService.getOrCreate(request.getRoleId());
            observer.onNext(KnightsHandbookResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setHandbook(toHandbookData(h))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[KnightsGrpc] GetOrCreate error", e);
            observer.onNext(KnightsHandbookResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimSeqReward(ClaimSeqRewardRequest request, StreamObserver<KnightsHandbookResponse> observer) {
        log.info("[KnightsGrpc] ClaimSeqReward: roleId={} seqIndex={}", request.getRoleId(), request.getSeqIndex());
        try {
            KnightsHandbook h = knightsService.claimSeqReward(request.getRoleId(), request.getSeqIndex());
            observer.onNext(KnightsHandbookResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setHandbook(toHandbookData(h))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[KnightsGrpc] ClaimSeqReward error", e);
            observer.onNext(KnightsHandbookResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimLevelReward(ClaimLevelRewardRequest request, StreamObserver<KnightsHandbookResponse> observer) {
        log.info("[KnightsGrpc] ClaimLevelReward: roleId={} levelIndex={}", request.getRoleId(), request.getLevelIndex());
        try {
            KnightsHandbook h = knightsService.claimLevelReward(request.getRoleId(), request.getLevelIndex());
            observer.onNext(KnightsHandbookResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setHandbook(toHandbookData(h))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[KnightsGrpc] ClaimLevelReward error", e);
            observer.onNext(KnightsHandbookResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getConditions(GetConditionsRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[KnightsGrpc] GetConditions: roleId={}", request.getRoleId());
        try {
            Map<String, Object> conditions = knightsService.getConditions(request.getRoleId());
            String json = objectMapper.writeValueAsString(conditions);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[KnightsGrpc] GetConditions error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private KnightsHandbookData toHandbookData(KnightsHandbook h) {
        return KnightsHandbookData.newBuilder()
                .setId(h.getId() != null ? h.getId() : 0L)
                .setRoleId(h.getRoleId() != null ? h.getRoleId() : 0L)
                .setLevel(h.getLevel() != null ? h.getLevel() : 0)
                .setFlag(h.getFlag() != null ? h.getFlag() : 0L)
                .setLevelFlag(h.getLevelFlag() != null ? h.getLevelFlag() : 0L)
                .build();
    }
}
