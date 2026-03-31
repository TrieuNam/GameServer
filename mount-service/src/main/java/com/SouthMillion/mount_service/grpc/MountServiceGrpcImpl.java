package com.SouthMillion.mount_service.grpc;

import com.SouthMillion.mount_service.dto.MountDTO;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.service.MountService;
import org.SouthMillion.proto.mount.*;
import org.SouthMillion.grpc.common.ResponseStatus;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mount gRPC Service Implementation
 * Provides high-performance mount operations via gRPC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MountServiceGrpcImpl extends MountServiceGrpc.MountServiceImplBase {

    private final MountService mountService;

    @Override
    public void getUserMounts(GetUserMountsRequest request, StreamObserver<GetUserMountsResponse> responseObserver) {
        log.info("[MountGrpc] GetUserMounts: userId={}", request.getUserId());
        
        try {
            List<Mount> mounts = mountService.getAllMounts(request.getUserId());
            
            GetUserMountsResponse.Builder responseBuilder = GetUserMountsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Success");
            
            for (Mount mount : mounts) {
                responseBuilder.addMounts(toMountData(mount));
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error getting user mounts", e);
            responseObserver.onNext(GetUserMountsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getMount(GetMountRequest request, StreamObserver<MountResponse> responseObserver) {
        log.info("[MountGrpc] GetMount: userId={}, index={}", request.getUserId(), request.getMountIndex());
        
        try {
            Mount mount = mountService.getMount(request.getUserId(), request.getMountIndex());
            
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Success")
                    .setMount(toMountData(mount))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error getting mount", e);
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unlockMount(UnlockMountRequest request, StreamObserver<MountResponse> responseObserver) {
        log.info("[MountGrpc] UnlockMount: userId={}, mountId={}", 
                request.getUserId(), request.getMountId());
        
        try {
            Mount mount = mountService.unlockMount(request.getUserId(), request.getMountId());
            
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount unlocked successfully")
                    .setMount(toMountData(mount))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error unlocking mount", e);
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void levelUpMount(LevelUpMountRequest request, StreamObserver<MountResponse> responseObserver) {
        log.info("[MountGrpc] LevelUpMount: userId={}, index={}, materials={}", 
                request.getUserId(), request.getMountIndex(), request.getMaterialIdsList().size());
        
        try {
            Mount mount = mountService.levelUpMount(request.getUserId(), request.getMountIndex());
            
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount leveled up successfully")
                    .setMount(toMountData(mount))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error leveling up mount", e);
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void gradeUpMount(GradeUpMountRequest request, StreamObserver<MountResponse> responseObserver) {
        log.info("[MountGrpc] GradeUpMount: userId={}, index={}", 
                request.getUserId(), request.getMountIndex());
        
        try {
            Mount mount = mountService.gradeUpMount(request.getUserId(), request.getMountIndex());
            
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount grade upgraded successfully")
                    .setMount(toMountData(mount))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error upgrading mount grade", e);
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void equipMount(EquipMountRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.info("[MountGrpc] EquipMount: userId={}, index={}", 
                request.getUserId(), request.getMountIndex());
        
        try {
            mountService.equipMount(request.getUserId(), request.getMountIndex());
            
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount equipped successfully")
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error equipping mount", e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unequipMount(UnequipMountRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.info("[MountGrpc] UnequipMount: userId={}", request.getUserId());
        
        try {
            mountService.unequipMount(request.getUserId());
            
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount unequipped successfully")
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error unequipping mount", e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void upgradeStarLevel(UpgradeStarLevelRequest request, StreamObserver<MountResponse> responseObserver) {
        log.info("[MountGrpc] UpgradeStarLevel: userId={}, index={}", 
                request.getUserId(), request.getMountIndex());
        
        try {
            Mount mount = mountService.upgradeStarLevel(request.getUserId(), request.getMountIndex());
            
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Mount star level upgraded successfully")
                    .setMount(toMountData(mount))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("[MountGrpc] Error upgrading star level", e);
            responseObserver.onNext(MountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * Convert Mount entity to proto MountData
     */
    private MountData toMountData(Mount mount) {
        MountData.Builder builder = MountData.newBuilder()
                .setId(mount.getId())
                .setUserId(mount.getUserId() != null ? mount.getUserId() : 0L)
                .setMountIndex(mount.getMountIndex())
                .setMountId(mount.getMountId())
                .setLevel(mount.getLevel())
                .setGrade(mount.getGrade())
                .setExp(mount.getExp())
                .setIsActive(mount.getIsActive())
                .setIsEquipped(mount.getIsEquipped())
                .setSkinLevel(mount.getSkinLevel())
                .setExploreProgress(mount.getExploreProgress());
        
        if (mount.getAppearanceId() != null) {
            builder.setAppearanceId(mount.getAppearanceId());
        }
        if (mount.getSkinId() != null) {
            builder.setSkinId(mount.getSkinId());
        }
        if (mount.getHarnessSlot1() != null) {
            builder.setHarnessSlot1(mount.getHarnessSlot1());
        }
        if (mount.getHarnessSlot2() != null) {
            builder.setHarnessSlot2(mount.getHarnessSlot2());
        }
        if (mount.getHarnessSlot3() != null) {
            builder.setHarnessSlot3(mount.getHarnessSlot3());
        }
        if (mount.getHarnessSlot4() != null) {
            builder.setHarnessSlot4(mount.getHarnessSlot4());
        }
        
        // Calculate combat power
        Long combatPower = mountService.calculateMountPower(mount);
        builder.setCombatPower(combatPower);
        
        if (mount.getCreatedAt() != null) {
            builder.setCreatedAt(mount.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        }
        if (mount.getUpdatedAt() != null) {
            builder.setUpdatedAt(mount.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        }
        
        return builder.build();
    }
}
