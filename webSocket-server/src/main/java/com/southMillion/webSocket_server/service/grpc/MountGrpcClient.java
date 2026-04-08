package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.mount.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MountGrpcClient {

    @GrpcClient("mount-service")
    private MountServiceGrpc.MountServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetUserMountsResponse getUserMounts(long userId) {
        try {
            return stub.getUserMounts(GetUserMountsRequest.newBuilder().setUserId(userId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] getUserMounts: mount-service unavailable");
            else log.error("[grpc-mount] getUserMounts error: {}", e.getMessage());
            return GetUserMountsResponse.newBuilder().setSuccess(false).build();
        }
    }

    public MountResponse unlockMount(long userId, int mountId) {
        try {
            return stub.unlockMount(UnlockMountRequest.newBuilder().setUserId(userId).setMountId(mountId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] unlockMount: mount-service unavailable");
            else log.error("[grpc-mount] unlockMount error: {}", e.getMessage());
            return MountResponse.newBuilder().setSuccess(false).build();
        }
    }

    public MountResponse levelUpMount(long userId, int mountIndex, List<Long> materialIds) {
        try {
            return stub.levelUpMount(LevelUpMountRequest.newBuilder()
                    .setUserId(userId)
                    .setMountIndex(mountIndex)
                    .addAllMaterialIds(materialIds)
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] levelUpMount: mount-service unavailable");
            else log.error("[grpc-mount] levelUpMount error: {}", e.getMessage());
            return MountResponse.newBuilder().setSuccess(false).build();
        }
    }

    public MountResponse gradeUpMount(long userId, int mountIndex) {
        try {
            return stub.gradeUpMount(GradeUpMountRequest.newBuilder().setUserId(userId).setMountIndex(mountIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] gradeUpMount: mount-service unavailable");
            else log.error("[grpc-mount] gradeUpMount error: {}", e.getMessage());
            return MountResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus equipMount(long userId, int mountIndex) {
        try {
            return stub.equipMount(EquipMountRequest.newBuilder().setUserId(userId).setMountIndex(mountIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] equipMount: mount-service unavailable");
            else log.error("[grpc-mount] equipMount error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus unequipMount(long userId) {
        try {
            return stub.unequipMount(UnequipMountRequest.newBuilder().setUserId(userId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] unequipMount: mount-service unavailable");
            else log.error("[grpc-mount] unequipMount error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public MountResponse upgradeStarLevel(long userId, int mountIndex) {
        try {
            return stub.upgradeStarLevel(UpgradeStarLevelRequest.newBuilder().setUserId(userId).setMountIndex(mountIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mount] upgradeStarLevel: mount-service unavailable");
            else log.error("[grpc-mount] upgradeStarLevel error: {}", e.getMessage());
            return MountResponse.newBuilder().setSuccess(false).build();
        }
    }
}
