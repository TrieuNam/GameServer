package com.SouthMillion.lingzhu_service.grpc;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import com.SouthMillion.lingzhu_service.service.LingZhuService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.lingzhu.*;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class LingZhuServiceGrpcImpl extends LingZhuServiceGrpc.LingZhuServiceImplBase {

    private final LingZhuService lingZhuService;

    @Override
    public void getAll(GetAllRequest request, StreamObserver<GetAllResponse> observer) {
        log.info("[LingZhuGrpc] GetAll: roleId={}", request.getRoleId());
        try {
            List<LingZhuProgress> list = lingZhuService.getAll(request.getRoleId());
            GetAllResponse.Builder resp = GetAllResponse.newBuilder().setSuccess(true).setMessage("OK");
            for (LingZhuProgress p : list) {
                resp.addItems(toLingZhuProgressData(p));
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[LingZhuGrpc] GetAll error", e);
            observer.onNext(GetAllResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void challenge(ChallengeRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[LingZhuGrpc] Challenge: roleId={} stage={} p1={}", request.getRoleId(), request.getStage(), request.getP1());
        try {
            Map<String, Object> result = lingZhuService.challenge(request.getRoleId(), request.getStage(), request.getP1());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder()
                    .setSuccess(ok)
                    .setMessage(ok ? "OK" : "failed")
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[LingZhuGrpc] Challenge error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void sweep(SweepRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[LingZhuGrpc] Sweep: roleId={} stage={} count={}", request.getRoleId(), request.getStage(), request.getCount());
        try {
            Map<String, Object> result = lingZhuService.sweep(request.getRoleId(), request.getStage(), request.getCount());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            observer.onNext(GenericResponse.newBuilder()
                    .setSuccess(ok)
                    .setMessage(ok ? "OK" : String.valueOf(result.getOrDefault("message", "failed")))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[LingZhuGrpc] Sweep error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private LingZhuProgressData toLingZhuProgressData(LingZhuProgress p) {
        return LingZhuProgressData.newBuilder()
                .setId(p.getId() != null ? p.getId() : 0L)
                .setRoleId(p.getRoleId() != null ? p.getRoleId() : 0L)
                .setStage(p.getStage() != null ? p.getStage() : 0)
                .setPassLevel(p.getPassLevel() != null ? p.getPassLevel() : 0)
                .setSweepCount(p.getSweepCount() != null ? p.getSweepCount() : 0)
                .build();
    }
}
