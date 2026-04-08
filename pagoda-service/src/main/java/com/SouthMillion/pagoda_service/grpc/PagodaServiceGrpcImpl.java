package com.SouthMillion.pagoda_service.grpc;

import com.SouthMillion.pagoda_service.entity.ShiLianProgress;
import com.SouthMillion.pagoda_service.service.PagodaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.pagoda.*;

import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PagodaServiceGrpcImpl extends PagodaServiceGrpc.PagodaServiceImplBase {

    private final PagodaService pagodaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void getShiLian(GetShiLianRequest request, StreamObserver<ShiLianResponse> observer) {
        log.info("[PagodaGrpc] GetShiLian: roleId={}", request.getRoleId());
        try {
            ShiLianProgress p = pagodaService.getShiLian(request.getRoleId());
            observer.onNext(ShiLianResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setShilian(toShiLianData(p))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] GetShiLian error", e);
            observer.onNext(ShiLianResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void challengeShiLian(ChallengeShiLianRequest request, StreamObserver<ShiLianResponse> observer) {
        log.info("[PagodaGrpc] ChallengeShiLian: roleId={} p1={}", request.getRoleId(), request.getP1());
        try {
            ShiLianProgress p = pagodaService.challengeShiLian(request.getRoleId(), request.getP1());
            observer.onNext(ShiLianResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setShilian(toShiLianData(p))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] ChallengeShiLian error", e);
            observer.onNext(ShiLianResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimShiLian(ClaimShiLianRequest request, StreamObserver<BoolResponse> observer) {
        log.info("[PagodaGrpc] ClaimShiLian: roleId={} p1={}", request.getRoleId(), request.getP1());
        try {
            boolean result = pagodaService.claimShiLian(request.getRoleId(), request.getP1());
            observer.onNext(BoolResponse.newBuilder().setSuccess(true).setMessage("OK").setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] ClaimShiLian error", e);
            observer.onNext(BoolResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getGuMo(GetGuMoRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[PagodaGrpc] GetGuMo: roleId={}", request.getRoleId());
        try {
            Map<String, Object> result = pagodaService.getGuMo(request.getRoleId());
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] GetGuMo error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void challengeGuMo(ChallengeGuMoRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[PagodaGrpc] ChallengeGuMo: roleId={} layerId={}", request.getRoleId(), request.getLayerId());
        try {
            Map<String, Object> result = pagodaService.challengeGuMo(request.getRoleId(), request.getLayerId());
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] ChallengeGuMo error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimGuMo(ClaimGuMoRequest request, StreamObserver<BoolResponse> observer) {
        log.info("[PagodaGrpc] ClaimGuMo: roleId={} layerId={}", request.getRoleId(), request.getLayerId());
        try {
            boolean result = pagodaService.claimGuMo(request.getRoleId(), request.getLayerId());
            observer.onNext(BoolResponse.newBuilder().setSuccess(true).setMessage("OK").setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[PagodaGrpc] ClaimGuMo error", e);
            observer.onNext(BoolResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private ShiLianData toShiLianData(ShiLianProgress p) {
        return ShiLianData.newBuilder()
                .setId(p.getId() != null ? p.getId() : 0L)
                .setRoleId(p.getRoleId() != null ? p.getRoleId() : 0L)
                .setPassLevel(p.getPassLevel() != null ? p.getPassLevel() : 0)
                .setBestLevel(p.getBestLevel() != null ? p.getBestLevel() : 0)
                .setUseItem(p.getUseItem() != null ? p.getUseItem() : 0)
                .setRandomId(p.getRandomId() != null ? p.getRandomId() : 0)
                .setSeasonEndTime(p.getSeasonEndTime() != null ? p.getSeasonEndTime() : 0L)
                .build();
    }
}
