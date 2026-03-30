package com.SouthMillion.role_service.grpc;

import com.SouthMillion.role_service.service.RoleService;
import com.SouthMillion.role_service.service.RoleSettingsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.role.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceGrpcImpl extends RoleServiceGrpc.RoleServiceImplBase {

    private final RoleService roleService;
    private final RoleSettingsService roleSettingsService;

    @Override
    public void getRole(GetRoleRequest request, StreamObserver<RoleResponse> responseObserver) {
        log.debug("gRPC GetRole called for roleId: {}", request.getRoleId());
        try {
            Optional<RoleDTOs.RoleResp> roleOpt = roleService.getById(request.getRoleId());
            if (roleOpt.isEmpty()) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Role not found: " + request.getRoleId())
                        .asRuntimeException());
                return;
            }
            responseObserver.onNext(buildRoleResponse(roleOpt.get(), request.getIncludeStats()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC GetRole: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getRolesByUserId(GetRolesByUserIdRequest request, StreamObserver<RoleListResponse> responseObserver) {
        log.debug("gRPC GetRolesByUserId called for userId: {}", request.getUserId());
        try {
            List<RoleResponse> roleResponses = roleService.listByUserId(request.getUserId())
                    .stream()
                    .map(r -> buildRoleResponse(r, false))
                    .collect(Collectors.toList());

            responseObserver.onNext(RoleListResponse.newBuilder()
                    .addAllRoles(roleResponses)
                    .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC GetRolesByUserId: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void addExp(AddExpRequest request, StreamObserver<AddExpResponse> responseObserver) {
        log.debug("gRPC AddExp called for roleId: {}, exp: {}", request.getRoleId(), request.getExpAmount());
        try {
            RoleDTOs.RoleResp resp = roleService.addExp(request.getRoleId(), request.getExpAmount());
            responseObserver.onNext(AddExpResponse.newBuilder()
                    .setRoleId(request.getRoleId())
                    .setNewLevel(resp.getLevel())
                    .setCurrentExp(resp.getCurExp())
                    .setLeveledUp(false)
                    .setStatus(ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build())
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in gRPC AddExp: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Error in gRPC AddExp: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void setWxInfo(SetWxInfoRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.debug("gRPC SetWxInfo called for roleId: {}", request.getRoleId());
        try {
            roleService.setWxInfo(request.getRoleId(), request.getName(), request.getHeadChar());
            responseObserver.onNext(ResponseStatus.newBuilder().setCode(200).setMessage("WX info updated").setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC SetWxInfo: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void applySettings(ApplySettingsRequest request, StreamObserver<ResponseStatus> responseObserver) {
        log.debug("gRPC ApplySettings called for roleId: {}", request.getRoleId());
        try {
            // Resolve userId from role
            Optional<RoleDTOs.RoleResp> roleOpt = roleService.getById(request.getRoleId());
            if (roleOpt.isEmpty()) {
                responseObserver.onNext(ResponseStatus.newBuilder().setCode(404).setMessage("Role not found").setSuccess(false).build());
                responseObserver.onCompleted();
                return;
            }
            String userId = roleOpt.get().getUserId();

            // Convert grpc settings map → SettingsDTOs.SystemSetReq
            List<SettingsDTOs.SystemSettingItem> items = request.getSettingsMap().entrySet().stream()
                    .map(e -> new SettingsDTOs.SystemSettingItem(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            roleSettingsService.applySettings(new SettingsDTOs.SystemSetReq(userId, items));

            responseObserver.onNext(ResponseStatus.newBuilder().setCode(200).setMessage("Settings applied").setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in gRPC ApplySettings: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public StreamObserver<RoleUpdateRequest> streamRoleUpdates(StreamObserver<RoleUpdateEvent> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(RoleUpdateRequest request) {
                try {
                    responseObserver.onNext(RoleUpdateEvent.newBuilder()
                            .setRoleId(request.getRoleId())
                            .setEventType(request.getUpdateType())
                            .setTimestamp(System.currentTimeMillis())
                            .build());
                } catch (Exception e) {
                    log.error("Error processing role update: {}", e.getMessage(), e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in StreamRoleUpdates: {}", t.getMessage(), t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private RoleResponse buildRoleResponse(RoleDTOs.RoleResp r, boolean includeStats) {
        RoleResponse.Builder b = RoleResponse.newBuilder()
                .setRoleId(r.getRoleId() != null ? parseLongSafe(r.getRoleId()) : 0)
                .setUserId(r.getUserId() != null ? r.getUserId() : "")
                .setRoleName(r.getName() != null ? r.getName() : "")
                .setLevel(r.getLevel())
                .setExp(r.getCurExp() != null ? r.getCurExp() : 0L);

        if (includeStats && r.getHp() != null) {
            b.setStats(RoleStats.newBuilder()
                    .setHp(r.getHp().intValue()).setMaxHp(r.getHp().intValue())
                    .setAttack(r.getAttack() != null ? r.getAttack().intValue() : 0)
                    .setDefense(r.getDefense() != null ? r.getDefense().intValue() : 0)
                    .setSpeed(r.getSpeed() != null ? r.getSpeed() : 0)
                    .setCritRate(10).setCritDamage(150)
                    .build());
        }
        return b.build();
    }

    private long parseLongSafe(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}
