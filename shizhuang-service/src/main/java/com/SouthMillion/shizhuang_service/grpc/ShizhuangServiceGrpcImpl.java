package com.SouthMillion.shizhuang_service.grpc;

import com.SouthMillion.shizhuang_service.service.ShizhuangService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.dto.ShiZhuang.ShizhuangDTOs;
import org.SouthMillion.proto.shizhuang.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ShizhuangServiceGrpcImpl extends ShizhuangServiceGrpc.ShizhuangServiceImplBase {

    private final ShizhuangService shizhuangService;

    @Override
    public void listByRole(RoleRequest request, StreamObserver<ShizhuangListResponse> observer) {
        log.info("[ShizhuangGrpc] ListByRole: roleId={}", request.getRoleId());
        try {
            ShizhuangDTOs.ShizhuangListResp resp = shizhuangService.listByRole(request.getRoleId());
            ShizhuangListResponse.Builder builder = ShizhuangListResponse.newBuilder().setSuccess(true).setMessage("OK");
            if (resp.getItems() != null) {
                resp.getItems().forEach(i -> builder.addItems(toShizhuangData(i)));
            }
            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ShizhuangGrpc] ListByRole error", e);
            observer.onNext(ShizhuangListResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getInfo(GetInfoRequest request, StreamObserver<ShizhuangInfoResponse> observer) {
        log.info("[ShizhuangGrpc] GetInfo: roleId={} shizhuangId={}", request.getRoleId(), request.getShizhuangId());
        try {
            ShizhuangDTOs.ShizhuangInfo info = shizhuangService.getInfo(request.getRoleId(), request.getShizhuangId());
            ShizhuangInfoResponse.Builder builder = ShizhuangInfoResponse.newBuilder().setSuccess(info != null).setMessage("OK");
            if (info != null) {
                builder.setInfo(toShizhuangData(info));
            }
            observer.onNext(builder.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ShizhuangGrpc] GetInfo error", e);
            observer.onNext(ShizhuangInfoResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<OperationResponse> observer) {
        log.info("[ShizhuangGrpc] Activate: roleId={} shizhuangId={}", request.getRoleId(), request.getShizhuangId());
        try {
            ShizhuangDTOs.ActivateReq req = ShizhuangDTOs.ActivateReq.builder()
                    .roleId(String.valueOf(request.getRoleId()))
                    .shizhuangId(request.getShizhuangId())
                    .build();
            ShizhuangDTOs.OperationResp resp = shizhuangService.activate(req);
            observer.onNext(OperationResponse.newBuilder()
                    .setSuccess(Boolean.TRUE.equals(resp.getOk()))
                    .setMessage(resp.getMessage() != null ? resp.getMessage() : "")
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ShizhuangGrpc] Activate error", e);
            observer.onNext(OperationResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void wear(WearRequest request, StreamObserver<OperationResponse> observer) {
        log.info("[ShizhuangGrpc] Wear: roleId={} shizhuangId={}", request.getRoleId(), request.getShizhuangId());
        try {
            ShizhuangDTOs.WearReq req = ShizhuangDTOs.WearReq.builder()
                    .roleId(String.valueOf(request.getRoleId()))
                    .shizhuangId(request.getShizhuangId())
                    .build();
            ShizhuangDTOs.OperationResp resp = shizhuangService.wear(req);
            observer.onNext(OperationResponse.newBuilder()
                    .setSuccess(Boolean.TRUE.equals(resp.getOk()))
                    .setMessage(resp.getMessage() != null ? resp.getMessage() : "")
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ShizhuangGrpc] Wear error", e);
            observer.onNext(OperationResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void levelUp(LevelUpRequest request, StreamObserver<OperationResponse> observer) {
        log.info("[ShizhuangGrpc] LevelUp: roleId={} shizhuangId={}", request.getRoleId(), request.getShizhuangId());
        try {
            ShizhuangDTOs.LevelUpReq req = ShizhuangDTOs.LevelUpReq.builder()
                    .roleId(String.valueOf(request.getRoleId()))
                    .shizhuangId(request.getShizhuangId())
                    .build();
            ShizhuangDTOs.OperationResp resp = shizhuangService.levelUp(req);
            observer.onNext(OperationResponse.newBuilder()
                    .setSuccess(Boolean.TRUE.equals(resp.getOk()))
                    .setMessage(resp.getMessage() != null ? resp.getMessage() : "")
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ShizhuangGrpc] LevelUp error", e);
            observer.onNext(OperationResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private ShizhuangData toShizhuangData(ShizhuangDTOs.ShizhuangInfo i) {
        return ShizhuangData.newBuilder()
                .setRoleId(i.getRoleId() != null ? Long.parseLong(i.getRoleId()) : 0L)
                .setShizhuangId(i.getShizhuangId())
                .setActivated(Boolean.TRUE.equals(i.getActivated()))
                .setWearing(Boolean.TRUE.equals(i.getWearing()))
                .setLevel(i.getLevel())
                .build();
    }
}
