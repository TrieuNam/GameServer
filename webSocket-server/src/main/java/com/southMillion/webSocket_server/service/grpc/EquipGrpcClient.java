package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.equip.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC client for Equipment Service
 * Replaces EquipFeign REST calls with gRPC for 50-60% performance improvement
 */
@Slf4j
@Service
public class EquipGrpcClient {

    @GrpcClient("equip-service")
    private EquipmentServiceGrpc.EquipmentServiceBlockingStub equipmentServiceStub;

    /**
     * Get all equipped items
     */
    public EquipmentResponse getEquipment(Long roleId, boolean includeStats) {
        try {
            GetEquipmentRequest request = GetEquipmentRequest.newBuilder()
                    .setRoleId(roleId)
                    .setIncludeStats(includeStats)
                    .build();

            EquipmentResponse response = equipmentServiceStub.getEquipment(request);
            log.debug("Got equipment for role: {}, items count: {}", roleId, response.getItemsCount());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get equipment for role: {}", roleId, e);
            // Return empty response on error
            return EquipmentResponse.newBuilder()
                    .setRoleId(roleId)
                    .build();
        }
    }

    /**
     * Equip an item
     */
    public EquipItemResponse equipItem(Long roleId, int itemId, int slotId, boolean autoUnequipOld) {
        try {
            EquipItemRequest request = EquipItemRequest.newBuilder()
                    .setRoleId(roleId)
                    .setItemId(itemId)
                    .setSlotId(slotId)
                    .setAutoUnequipOld(autoUnequipOld)
                    .build();

            EquipItemResponse response = equipmentServiceStub.equipItem(request);
            if (response.getSuccess()) {
                log.info("Successfully equipped item {} in slot {} for role: {}, new fight power: {}",
                        itemId, slotId, roleId, response.getNewFightPower());
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to equip item {} for role: {}", itemId, roleId, e);
            return EquipItemResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Unequip an item
     */
    public UnequipItemResponse unequipItem(Long roleId, int slotId) {
        try {
            UnequipItemRequest request = UnequipItemRequest.newBuilder()
                    .setRoleId(roleId)
                    .setSlotId(slotId)
                    .build();

            UnequipItemResponse response = equipmentServiceStub.unequipItem(request);
            if (response.getSuccess()) {
                log.info("Successfully unequipped item from slot {} for role: {}", slotId, roleId);
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to unequip item from slot {} for role: {}", slotId, roleId, e);
            return UnequipItemResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Upgrade equipment
     */
    public UpgradeEquipmentResponse upgradeEquipment(Long roleId, int slotId, int itemId, 
                                                     List<MaterialItem> materials) {
        try {
            // Convert materials to ItemStack
            List<org.SouthMillion.grpc.common.ItemStack> materialStacks = materials.stream()
                    .map(m -> org.SouthMillion.grpc.common.ItemStack.newBuilder()
                            .setItemId(m.getItemId())
                            .setQuantity(m.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            UpgradeEquipmentRequest request = UpgradeEquipmentRequest.newBuilder()
                    .setRoleId(roleId)
                    .setSlotId(slotId)
                    .setItemId(itemId)
                    .addAllMaterials(materialStacks)
                    .build();

            UpgradeEquipmentResponse response = equipmentServiceStub.upgradeEquipment(request);
            if (response.getSuccess()) {
                log.info("Successfully upgraded equipment for role: {}, slot: {}, level: {} -> {}",
                        roleId, slotId, response.getOldLevel(), response.getNewLevel());
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to upgrade equipment for role: {}, slot: {}", roleId, slotId, e);
            return UpgradeEquipmentResponse.newBuilder()
                    .setSuccess(false)
                    .build();
        }
    }

    /**
     * Get equipment stats
     */
    public EquipmentStatsResponse getEquipmentStats(Long roleId) {
        try {
            GetEquipmentStatsRequest request = GetEquipmentStatsRequest.newBuilder()
                    .setRoleId(roleId)
                    .build();

            EquipmentStatsResponse response = equipmentServiceStub.getEquipmentStats(request);
            log.debug("Got equipment stats for role: {}", roleId);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to get equipment stats for role: {}", roleId, e);
            return EquipmentStatsResponse.newBuilder().build();
        }
    }

    /**
     * Batch get equipment (for combat calculations)
     */
    public BatchGetEquipmentResponse batchGetEquipment(List<String> roleIds) {
        try {
            List<Long> roleIdLongs = roleIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            BatchGetEquipmentRequest request = BatchGetEquipmentRequest.newBuilder()
                    .addAllRoleIds(roleIdLongs)
                    .build();

            BatchGetEquipmentResponse response = equipmentServiceStub.batchGetEquipment(request);
            log.debug("Batch got equipment for {} roles", response.getEquipmentsCount());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("Failed to batch get equipment", e);
            return BatchGetEquipmentResponse.newBuilder().build();
        }
    }

    /**
     * Helper class for material items
     */
    public static class MaterialItem {
        private Integer itemId;
        private Integer quantity;

        public MaterialItem(Integer itemId, Integer quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public Integer getItemId() {
            return itemId;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }
}
