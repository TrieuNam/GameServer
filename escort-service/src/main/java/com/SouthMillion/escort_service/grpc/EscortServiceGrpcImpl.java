package com.SouthMillion.escort_service.grpc;

import com.SouthMillion.escort_service.model.entity.EscortMission;
import com.SouthMillion.escort_service.model.entity.EscortStats;
import com.SouthMillion.escort_service.service.EscortService;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.escort.*;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * gRPC server implementation for Escort Service.
 * Called by webSocket-server via EscortGrpcClient.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EscortServiceGrpcImpl extends EscortServiceGrpc.EscortServiceImplBase {

    private final EscortService escortService;

    // ─── GetEscortInfo ─────────────────────────────────────────────────────────
    // SC:9622 PB_SCEscortRoleInfo — called on login push and op=1
    @Override
    public void getEscortInfo(EscortInfoRequest request,
                              StreamObserver<EscortInfoResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] getEscortInfo roleId={}", roleId);
        try {
            EscortStats stats = escortService.initializeStats(roleId);

            EscortRoleStats grpcStats = EscortRoleStats.newBuilder()
                    .setShip(safeInt(stats.getCurrentShipLevel(), 1))
                    .setEscortCount(safeInt(stats.getEscortCount(), 0))
                    .setInterceptCount(safeInt(stats.getInterceptCount(), 0))
                    .setHelpCount(safeInt(stats.getHelpCount(), 0))
                    .setMaxDamage(0)
                    .setRewardIndex(safeInt(stats.getRewardsClaimed(), 0))
                    .build();

            responseObserver.onNext(EscortInfoResponse.newBuilder().setStats(grpcStats).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] getEscortInfo error roleId={}", roleId, e);
            responseObserver.onNext(EscortInfoResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    // ─── GetShipList ───────────────────────────────────────────────────────────
    // SC:9623 PB_SCEscortShipListInfo — called on op=2
    @Override
    public void getShipList(EscortInfoRequest request,
                            StreamObserver<EscortShipListResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] getShipList roleId={}", roleId);
        try {
            EscortStats stats = escortService.initializeStats(roleId);
            int shipLevel = safeInt(stats.getCurrentShipLevel(), 1);

            // be_intercept=int32, is_help=int32 → use 0/1
            EscortShipData myShip = EscortShipData.newBuilder()
                    .setShip(shipLevel).setUid(0)
                    .setBeIntercept(0).setOverTime(0).setShipKey(0).setIsHelp(0)
                    .build();

            EscortShipListResponse.Builder builder = EscortShipListResponse.newBuilder().setMyShip(myShip);

            List<EscortMission> active = escortService.getActiveMissions(roleId);
            for (EscortMission m : active) {
                int overtime = 0;
                if (m.getExpiryTime() != null) {
                    overtime = (int) java.time.Instant.now()
                            .until(m.getExpiryTime().toInstant(ZoneOffset.UTC), ChronoUnit.SECONDS);
                }
                builder.addShipList(EscortShipData.newBuilder()
                        .setShip(shipLevel).setUid(0)
                        .setBeIntercept(0)
                        .setOverTime(Math.max(0, overtime))
                        .setShipKey(safeInt(m.getShipKey(), 0))
                        .setIsHelp(0)
                        .build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] getShipList error roleId={}", roleId, e);
            responseObserver.onNext(EscortShipListResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    // ─── StartEscort ───────────────────────────────────────────────────────────
    // SC:9621 PB_SCEscortRet — called on op=3
    @Override
    public void startEscort(StartEscortRequest request,
                            StreamObserver<EscortActionResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] startEscort roleId={} shipLevel={}", roleId, request.getShipLevel());
        try {
            List<EscortMission> available = escortService.getAllMissions(roleId).stream()
                    .filter(m -> m.getStatus() == 0)
                    .toList();

            EscortMission mission;
            if (available.isEmpty()) {
                mission = escortService.generateMission(roleId, 1);
            } else {
                mission = available.get(0);
            }

            EscortMission started = escortService.startMission(roleId, mission.getId());
            responseObserver.onNext(EscortActionResponse.newBuilder()
                    .setSuccess(true)
                    .setP1(safeInt(started.getMissionId(), 0))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] startEscort error roleId={}", roleId, e);
            responseObserver.onNext(EscortActionResponse.newBuilder().setSuccess(false).setP1(0).build());
            responseObserver.onCompleted();
        }
    }

    // ─── InterceptEscort ───────────────────────────────────────────────────────
    // SC:9621 PB_SCEscortRet — called on op=5 (rob)
    @Override
    public void interceptEscort(InterceptRequest request,
                                StreamObserver<EscortActionResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        long targetUid = request.getTargetUid();
        log.debug("[EscortGrpc] interceptEscort roleId={} targetUid={}", roleId, targetUid);
        try {
            // Persist intercept count to DB
            escortService.recordIntercept(roleId);
            responseObserver.onNext(EscortActionResponse.newBuilder()
                    .setSuccess(true).setP1((int) targetUid).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] interceptEscort error roleId={}", roleId, e);
            responseObserver.onNext(EscortActionResponse.newBuilder().setSuccess(false).setP1(0).build());
            responseObserver.onCompleted();
        }
    }

    // ─── HelpEscort ────────────────────────────────────────────────────────────
    // SC:9621 PB_SCEscortRet — called on op=6 (speedup)
    @Override
    public void helpEscort(HelpRequest request,
                           StreamObserver<EscortActionResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        long targetUid = request.getTargetUid();
        log.debug("[EscortGrpc] helpEscort roleId={} targetUid={}", roleId, targetUid);
        try {
            // Persist help count to DB
            escortService.recordHelp(roleId);
            responseObserver.onNext(EscortActionResponse.newBuilder()
                    .setSuccess(true).setP1((int) targetUid).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] helpEscort error roleId={}", roleId, e);
            responseObserver.onNext(EscortActionResponse.newBuilder().setSuccess(false).setP1(0).build());
            responseObserver.onCompleted();
        }
    }

    // ─── ClaimReward ───────────────────────────────────────────────────────────
    // SC:9621 PB_SCEscortRet — called on op=4 (complete)
    @Override
    public void claimReward(EscortInfoRequest request,
                            StreamObserver<EscortActionResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] claimReward roleId={}", roleId);
        try {
            // Auto-complete any in-progress missions before looking for rewards
            // (client may not send a separate completion request in the C++ protocol)
            escortService.autoCompleteMissions(roleId);

            List<EscortMission> unclaimed = escortService.getUnclaimedRewards(roleId);
            if (unclaimed.isEmpty()) {
                responseObserver.onNext(EscortActionResponse.newBuilder()
                        .setSuccess(false).setP1(-1).build());
                responseObserver.onCompleted();
                return;
            }

            EscortMission claimed = escortService.claimReward(roleId, unclaimed.get(0).getId());

            // Persist escort_count, rewards_claimed and totalCompleted atomically
            EscortStats stats = escortService.initializeStats(roleId);
            stats.setEscortCount(safeInt(stats.getEscortCount(), 0) + 1);
            stats.setRewardsClaimed(safeInt(stats.getRewardsClaimed(), 0) + 1);
            stats.setTotalCompleted(safeInt(stats.getTotalCompleted(), 0) + 1); // align with escortCount ← Session 10 fix
            escortService.saveStats(stats);

            responseObserver.onNext(EscortActionResponse.newBuilder()
                    .setSuccess(true)
                    .setP1(safeInt(claimed.getMissionId(), 0))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] claimReward error roleId={}", roleId, e);
            responseObserver.onNext(EscortActionResponse.newBuilder().setSuccess(false).setP1(0).build());
            responseObserver.onCompleted();
        }
    }

    // ─── GetReportList ─────────────────────────────────────────────────────────
    // SC:9624 PB_SCEscortReportListInfo — called on op=7
    @Override
    public void getReportList(EscortInfoRequest request,
                              StreamObserver<EscortReportListResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] getReportList roleId={}", roleId);
        try {
            List<EscortMission> completed = escortService.getCompletedMissions(roleId);
            EscortReportListResponse.Builder builder = EscortReportListResponse.newBuilder();
            for (EscortMission m : completed) {
                // report_list = repeated bytes → encode missionId as 4-byte ByteString
                int missionId = safeInt(m.getMissionId(), 0);
                builder.addReportList(ByteString.copyFrom(new byte[]{
                        (byte)(missionId >> 24), (byte)(missionId >> 16),
                        (byte)(missionId >> 8),  (byte) missionId}));
                long ts = m.getCompletionTime() != null
                        ? m.getCompletionTime().toEpochSecond(ZoneOffset.UTC) : 0L;
                builder.addReportTime(ts);
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] getReportList error roleId={}", roleId, e);
            responseObserver.onNext(EscortReportListResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    // ─── GetInterceptList ──────────────────────────────────────────────────────
    // SC:9625 PB_SCEscortInterceptListInfo — called on op=8
    @Override
    public void getInterceptList(EscortInfoRequest request,
                                 StreamObserver<EscortInterceptListResponse> responseObserver) {
        String roleId = String.valueOf(request.getRoleId());
        log.debug("[EscortGrpc] getInterceptList roleId={}", roleId);
        try {
            List<EscortMission> active = escortService.getActiveMissions(roleId);
            EscortStats stats = escortService.initializeStats(roleId);
            int shipLevel = safeInt(stats.getCurrentShipLevel(), 1);

            EscortInterceptListResponse.Builder builder = EscortInterceptListResponse.newBuilder();
            for (EscortMission m : active) {
                int overtime = 0;
                if (m.getExpiryTime() != null) {
                    overtime = (int) java.time.Instant.now()
                            .until(m.getExpiryTime().toInstant(ZoneOffset.UTC), ChronoUnit.SECONDS);
                }
                EscortShipData ship = EscortShipData.newBuilder()
                        .setShip(shipLevel).setUid(0)
                        .setBeIntercept(0)
                        .setOverTime(Math.max(0, overtime))
                        .setShipKey(safeInt(m.getShipKey(), 0))
                        .setIsHelp(0)
                        .build();
                builder.addList(EscortInterceptData.newBuilder().setShip(ship).build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[EscortGrpc] getInterceptList error roleId={}", roleId, e);
            responseObserver.onNext(EscortInterceptListResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private static int safeInt(Integer val, int defaultVal) {
        return val != null ? val : defaultVal;
    }
}
