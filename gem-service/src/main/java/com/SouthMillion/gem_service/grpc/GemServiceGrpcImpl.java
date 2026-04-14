package com.SouthMillion.gem_service.grpc;

import com.SouthMillion.gem_service.entity.GemData;
import com.SouthMillion.gem_service.service.GemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.gem.*;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GemServiceGrpcImpl extends GemServiceGrpc.GemServiceImplBase {

    private final GemService gemService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void getGemInfo(GetGemInfoRequest request, StreamObserver<GemInfoResponse> observer) {
        log.info("[GemGrpc] GetGemInfo: roleId={}", request.getRoleId());
        try {
            Map<String, Object> info = gemService.getGemInfo(request.getRoleId());
            GemInfoResponse.Builder resp = GemInfoResponse.newBuilder().setSuccess(true).setMessage("OK");
            Object drawingId = info.get("drawingId");
            resp.setDrawingId(drawingId instanceof Number ? ((Number) drawingId).intValue() : -1);
            Object gems = info.get("gems");
            if (gems instanceof List<?> gemList) {
                for (Object g : gemList) {
                    if (g instanceof GemData gd) {
                        resp.addGems(toGemData(gd));
                    }
                }
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] GetGemInfo error", e);
            observer.onNext(GemInfoResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void inlay(InlayRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Inlay: roleId={} gemId={} slot={}", request.getRoleId(), request.getGemId(), request.getSlotType());
        try {
            Map<String, Object> result = gemService.inlay(request.getRoleId(), request.getGemId(), request.getSlotType());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok).setMessage(ok ? "OK" : "failed").build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Inlay error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void remove(RemoveRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Remove: roleId={} gemId={}", request.getRoleId(), request.getGemId());
        try {
            Map<String, Object> result = gemService.remove(request.getRoleId(), request.getGemId());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok).setMessage(ok ? "OK" : "failed").build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Remove error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void compose(ComposeRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Compose: roleId={} sources={}", request.getRoleId(), request.getSourceGemIdsCount());
        try {
            Map<String, Object> result = gemService.compose(request.getRoleId(), request.getSourceGemIdsList());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok).setMessage(ok ? "OK" : "failed").build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Compose error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void upgradeAll(UpgradeAllRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] UpgradeAll: roleId={} count={}", request.getRoleId(), request.getGemIdsCount());
        try {
            Map<String, Object> result = gemService.upgradeAll(request.getRoleId(), request.getGemIdsList());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok).setMessage(ok ? "OK" : "failed").build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] UpgradeAll error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void buy(BuyRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Buy: roleId={} count={}", request.getRoleId(), request.getGemIdsCount());
        try {
            Map<String, Object> result = gemService.buy(request.getRoleId(), request.getGemIdsList());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok).setMessage(ok ? "OK" : "failed").build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Buy error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void move(MoveRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Move: roleId={} drawingId={} gemIndex={} x={} y={}",
                request.getRoleId(), request.getDrawingId(), request.getGemIndex(), request.getX(), request.getY());
        try {
            Map<String, Object> result = gemService.move(
                    request.getRoleId(), request.getDrawingId(),
                    request.getGemIndex(), request.getX(), request.getY());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok)
                    .setMessage(ok ? "OK" : String.valueOf(result.get("reason"))).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Move error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void transform(TransformRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] Transform: roleId={} source={} target={}",
                request.getRoleId(), request.getSourceGemId(), request.getTargetGemId());
        try {
            Map<String, Object> result = gemService.transform(
                    request.getRoleId(), request.getSourceGemId(), request.getTargetGemId());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok)
                    .setMessage(ok ? "OK" : String.valueOf(result.get("reason"))).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] Transform error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void levelUp(LevelUpRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[GemGrpc] LevelUp: roleId={} gemId={}", request.getRoleId(), request.getGemId());
        try {
            Map<String, Object> result = gemService.levelUp(request.getRoleId(), request.getGemId());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder().setSuccess(ok)
                    .setMessage(ok ? "OK" : String.valueOf(result.get("reason"))).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[GemGrpc] LevelUp error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private org.SouthMillion.proto.gem.GemData toGemData(GemData g) {
        return org.SouthMillion.proto.gem.GemData.newBuilder()
                .setId(g.getId() != null ? g.getId() : 0L)
                .setRoleId(g.getRoleId() != null ? g.getRoleId() : 0L)
                .setGemId(g.getGemId() != null ? g.getGemId() : 0)
                .setLevel(g.getLevel() != null ? g.getLevel() : 0)
                .setCount(g.getCount() != null ? g.getCount() : 0)
                .setIsInlaid(Boolean.TRUE.equals(g.getIsInlaid()))
                .setSlotType(g.getSlotType() != null ? g.getSlotType() : -1)
                .build();
    }
}
