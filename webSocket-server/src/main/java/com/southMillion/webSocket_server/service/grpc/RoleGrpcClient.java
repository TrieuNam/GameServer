package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.grpc.role.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Client Service for role-service
 * Replaces REST Feign calls with high-performance gRPC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleGrpcClient.class);

    @GrpcClient("role-service")
    private RoleServiceGrpc.RoleServiceBlockingStub roleServiceStub;

    /**
     * Get role information
     */
    public RoleResponse getRole(Long roleId) {
        try {
            GetRoleRequest request = GetRoleRequest.newBuilder()
                    .setRoleId(roleId)
                    .build();

            return roleServiceStub.getRole(request);

        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to get role for roleId={}: {}", roleId, e.getMessage());
            return null;
        }
    }

    /**
     * Add experience to role
     */
    public AddExpResponse addExp(Long roleId, long exp, String source) {
        try {
            AddExpRequest request = AddExpRequest.newBuilder()
                    .setRoleId(roleId)
                    .setExpAmount(exp)
                    .setSource(source != null ? source : "manual")
                    .build();

            return roleServiceStub.addExp(request);

        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to add exp for roleId={}: {}", roleId, e.getMessage());
            return null;
        }
    }

    /**
     * Update role (custom fields)
     */
    public RoleResponse updateRole(Long roleId, Integer vipLevel, Integer fightPower) {
        try {
            UpdateRoleRequest.Builder builder = UpdateRoleRequest.newBuilder()
                    .setRoleId(roleId);
            
            if (vipLevel != null) {
                builder.setVipLevel(vipLevel);
            }
            if (fightPower != null) {
                builder.setFightPower(fightPower);
            }

            return roleServiceStub.updateRole(builder.build());

        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to update role for roleId={}: {}", roleId, e.getMessage());
            return null;
        }
    }

    /**
     * Batch get roles
     */
    public BatchGetRolesResponse batchGetRoles(List<String> roleIds) {
        try {
            BatchGetRolesRequest.Builder builder = BatchGetRolesRequest.newBuilder();
            roleIds.forEach(id -> builder.addRoleIds(Long.parseLong(id)));

            return roleServiceStub.batchGetRoles(builder.build());

        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to batch get roles: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set WX info (WeChat profile)
     */
    public org.SouthMillion.grpc.common.ResponseStatus setWxInfo(
            org.SouthMillion.grpc.role.SetWxInfoRequest request) {
        try {
            return roleServiceStub.setWxInfo(request);
        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to set WX info: {}", e.getMessage());
            return org.SouthMillion.grpc.common.ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Failed: " + e.getMessage())
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Apply system settings
     */
    public org.SouthMillion.grpc.common.ResponseStatus applySettings(
            org.SouthMillion.grpc.role.ApplySettingsRequest request) {
        try {
            return roleServiceStub.applySettings(request);
        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to apply settings: {}", e.getMessage());
            return org.SouthMillion.grpc.common.ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Failed: " + e.getMessage())
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Get all roles for a user
     */
    public List<RoleDTOs.RoleResp> getRolesByUserId(String userId) {
        try {
            GetRolesByUserIdRequest request = GetRolesByUserIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            RoleListResponse response = roleServiceStub.getRolesByUserId(request);
            if (response == null) return Collections.emptyList();
            return response.getRolesList().stream()
                    .map(this::protoToRoleResp)
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-role] Failed to get roles for userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[grpc-role] Unexpected error getting roles for userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private RoleDTOs.RoleResp protoToRoleResp(RoleResponse r) {
        return RoleDTOs.RoleResp.builder()
                .roleId(r.getRoleId() != 0 ? String.valueOf(r.getRoleId()) : null)
                .userId(r.getUserId() != null && !r.getUserId().isEmpty() ? r.getUserId() : null)
                .name(r.getRoleName())
                .roleName(r.getRoleName())
                .level(r.getLevel() > 0 ? r.getLevel() : null)
                .curExp(r.getExp() > 0 ? r.getExp() : null)
                .cap((long) r.getFightPower())
                .createTimeEpochSec(r.getCreateTime() > 0 ? r.getCreateTime() : null)
                .build();
    }
}
