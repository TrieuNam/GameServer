package com.SouthMillion.activity_service.grpc;

import com.SouthMillion.activity_service.service.ActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.activity.*;

import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ActivityServiceGrpcImpl extends ActivityServiceGrpc.ActivityServiceImplBase {

    private final ActivityService activityService;
    private final ObjectMapper objectMapper;

    @Override
    public void handleRandActivity(RandActivityRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ActivityGrpc] HandleRandActivity: roleId={} type={}", request.getRoleId(), request.getActivityType());
        try {
            Map<String, Object> result = activityService.handleRandActivity(
                    request.getRoleId(),
                    request.getActivityType(),
                    0, 0, 0, 0);
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder()
                    .setSuccess(ok).setMessage(ok ? "OK" : "failed").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ActivityGrpc] HandleRandActivity error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getSevenDaySign(RoleRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ActivityGrpc] GetSevenDaySign: roleId={}", request.getRoleId());
        try {
            Object result = activityService.getSevenDay(request.getRoleId());
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ActivityGrpc] GetSevenDaySign error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimSevenDaySign(SignClaimRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ActivityGrpc] ClaimSevenDaySign: roleId={} day={}", request.getRoleId(), request.getDay());
        try {
            Object result = activityService.claimSevenDay(request.getRoleId(), request.getDay());
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ActivityGrpc] ClaimSevenDaySign error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getLuckUnpacking(RoleRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ActivityGrpc] GetLuckUnpacking: roleId={}", request.getRoleId());
        try {
            Object result = activityService.getLuck(request.getRoleId());
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ActivityGrpc] GetLuckUnpacking error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getActivityData(ActivityDataRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ActivityGrpc] GetActivityData: roleId={} type={}", request.getRoleId(), request.getActivityType());
        try {
            Map<String, Object> result = activityService.handleRandActivity(
                    request.getRoleId(),
                    request.getActivityType(),
                    0, 0, 0, 0);
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder().setSuccess(true).setMessage("OK").setDataJson(json).build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ActivityGrpc] GetActivityData error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }
}
