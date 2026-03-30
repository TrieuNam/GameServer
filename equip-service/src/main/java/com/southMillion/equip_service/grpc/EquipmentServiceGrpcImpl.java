package com.SouthMillion.equip_service.grpc;

import com.SouthMillion.equip_service.entity.EquipSlotEntity;
import com.SouthMillion.equip_service.repository.EquipSlotRepository;
import com.SouthMillion.equip_service.service.EquipService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.grpc.equip.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Equipment System
 * High-performance equipment operations via gRPC
 * 
 * Target Performance: <20ms per operation
 * Throughput: >2000 operations/sec
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EquipmentServiceGrpcImpl extends EquipmentServiceGrpc.EquipmentServiceImplBase {

    private final EquipService equipService;
    private final EquipSlotRepository slotRepository;

    @Override
    public void getEquipment(GetEquipmentRequest request,
                            StreamObserver<EquipmentResponse> responseObserver) {
        log.debug("gRPC GetEquipment: roleId={}", request.getRoleId());
        
        try {
            Long roleId = request.getRoleId();
            EquipDTOs.ListResp listResp = equipService.list(roleId);
            
            EquipmentResponse.Builder responseBuilder = EquipmentResponse.newBuilder()
                    .setRoleId(request.getRoleId())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build());
            
            // Convert EquipDTOs.EquipItem to proto EquippedItem
            if (listResp.getItems() != null) {
                for (EquipDTOs.EquipItem item : listResp.getItems()) {
                    EquippedItem.Builder itemBuilder = EquippedItem.newBuilder()
                            .setSlotId(item.getEquipType())
                            .setItemId(item.getItemId())
                            .setLevel(1)  // Level not in DTO, default to 1
                            .setQuality(1);  // Quality not in DTO, default to 1
                    
                    // Add attributes if available
                    if (item.getHp() > 0) {
                        itemBuilder.addAttributes(Attribute.newBuilder()
                                .setAttrType("HP")
                                .setValue(item.getHp())
                                .build());
                    }
                    if (item.getAttack() > 0) {
                        itemBuilder.addAttributes(Attribute.newBuilder()
                                .setAttrType("ATTACK")
                                .setValue(item.getAttack())
                                .build());
                    }
                    if (item.getDefend() > 0) {
                        itemBuilder.addAttributes(Attribute.newBuilder()
                                .setAttrType("DEFENSE")
                                .setValue(item.getDefend())
                                .build());
                    }
                    if (item.getSpeed() > 0) {
                        itemBuilder.addAttributes(Attribute.newBuilder()
                                .setAttrType("SPEED")
                                .setValue(item.getSpeed())
                                .build());
                    }
                    
                    responseBuilder.addItems(itemBuilder.build());
                }
            }
            
            // Calculate total stats if requested
            if (request.getIncludeStats() && listResp.getItems() != null) {
                TotalStats totalStats = calculateTotalStats(listResp.getItems());
                responseBuilder.setTotalStats(totalStats);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in GetEquipment: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get equipment: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void equipItem(EquipItemRequest request,
                         StreamObserver<EquipItemResponse> responseObserver) {
        log.debug("gRPC EquipItem: roleId={}, itemId={}, slotId={}", 
                request.getRoleId(), request.getItemId(), request.getSlotId());
        
        try {
            Long roleId = request.getRoleId();
            
            // Create DTO request
            EquipDTOs.EquipReq equipReq = new EquipDTOs.EquipReq();
            equipReq.setRoleId(String.valueOf(roleId));
            equipReq.setItemId(request.getItemId());
            
            // Execute equip operation
            EquipDTOs.OkResp result = equipService.equip(equipReq);
            
            EquipItemResponse.Builder responseBuilder = EquipItemResponse.newBuilder()
                    .setSuccess(result.isOk())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(result.isOk() ? 200 : 400)
                            .setMessage(result.message() != null ? result.message() : "OK")
                            .setSuccess(result.isOk())
                            .build());
            
            // Get updated equipment to return details
            if (result.isOk()) {
                EquipDTOs.ListResp listResp = equipService.list(roleId);
                if (listResp.getItems() != null) {
                    for (EquipDTOs.EquipItem item : listResp.getItems()) {
                        if (item.getItemId() == request.getItemId()) {
                            EquippedItem equippedItem = buildEquippedItem(item);
                            responseBuilder.setEquippedItem(equippedItem);
                            break;
                        }
                    }
                    
                    // Calculate new stats
                    TotalStats newStats = calculateTotalStats(listResp.getItems());
                    responseBuilder.setNewStats(newStats);
                    responseBuilder.setNewFightPower(newStats.getFightPowerBonus());
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in EquipItem: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to equip item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void unequipItem(UnequipItemRequest request,
                           StreamObserver<UnequipItemResponse> responseObserver) {
        log.debug("gRPC UnequipItem: roleId={}, slotId={}", request.getRoleId(), request.getSlotId());
        
        try {
            Long roleId = request.getRoleId();
            
            // Create DTO request
            EquipDTOs.UnequipReq unequipReq = new EquipDTOs.UnequipReq();
            unequipReq.setRoleId(String.valueOf(roleId));
            unequipReq.setEquipType(request.getSlotId());
            
            // Execute unequip operation
            EquipDTOs.OkResp result = equipService.unequip(unequipReq);
            
            UnequipItemResponse.Builder responseBuilder = UnequipItemResponse.newBuilder()
                    .setSuccess(result.isOk())
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(result.isOk() ? 200 : 400)
                            .setMessage(result.message() != null ? result.message() : "OK")
                            .setSuccess(result.isOk())
                            .build());
            
            // Get updated equipment stats
            if (result.isOk()) {
                EquipDTOs.ListResp listResp = equipService.list(roleId);
                if (listResp.getItems() != null) {
                    TotalStats newStats = calculateTotalStats(listResp.getItems());
                    responseBuilder.setNewStats(newStats);
                    responseBuilder.setNewFightPower(newStats.getFightPowerBonus());
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in UnequipItem: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to unequip item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void upgradeEquipment(UpgradeEquipmentRequest request,
                                StreamObserver<UpgradeEquipmentResponse> responseObserver) {
        log.debug("gRPC UpgradeEquipment: roleId={}, slotId={}", request.getRoleId(), request.getSlotId());
        
        try {
            Long roleId = request.getRoleId();
            int slotId = request.getSlotId();

            java.util.Optional<EquipSlotEntity> slotOpt = slotRepository.findByRoleIdAndEquipType(roleId, slotId);
            if (slotOpt.isEmpty()) {
                UpgradeEquipmentResponse response = UpgradeEquipmentResponse.newBuilder()
                        .setSuccess(false)
                        .setStatus(ResponseStatus.newBuilder()
                                .setCode(404)
                                .setMessage("Equipment slot not found: slotId=" + slotId)
                                .setSuccess(false)
                                .build())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            EquipSlotEntity slot = slotOpt.get();

            // Capture old stats for response
            int oldHp = slot.getHp();
            int oldAtk = slot.getAttack();
            int oldDef = slot.getDefend();
            int oldSpd = slot.getSpeed();

            // Enhancement: boost each stat by 10% (minimum +1)
            slot.setHp(oldHp + Math.max(1, oldHp / 10));
            slot.setAttack(oldAtk + Math.max(1, oldAtk / 10));
            slot.setDefend(oldDef + Math.max(1, oldDef / 10));
            slot.setSpeed(oldSpd + Math.max(1, oldSpd / 10));
            slotRepository.save(slot);

            // Build upgraded item proto
            EquipDTOs.EquipItem updatedItem = new EquipDTOs.EquipItem();
            updatedItem.setEquipType(slot.getEquipType());
            updatedItem.setItemId(slot.getItemId());
            updatedItem.setHp(slot.getHp());
            updatedItem.setAttack(slot.getAttack());
            updatedItem.setDefend(slot.getDefend());
            updatedItem.setSpeed(slot.getSpeed());

            TotalStats newStats = calculateTotalStats(
                    java.util.List.of(updatedItem)
            );

            UpgradeEquipmentResponse response = UpgradeEquipmentResponse.newBuilder()
                    .setSuccess(true)
                    .setUpgradedItem(buildEquippedItem(updatedItem))
                    .setNewStats(newStats)
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Equipment upgraded successfully")
                            .setSuccess(true)
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in UpgradeEquipment: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to upgrade equipment: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getEquipmentStats(GetEquipmentStatsRequest request,
                                 StreamObserver<EquipmentStatsResponse> responseObserver) {
        log.debug("gRPC GetEquipmentStats: roleId={}", request.getRoleId());
        
        try {
            Long roleId = request.getRoleId();
            EquipDTOs.ListResp listResp = equipService.list(roleId);
            
            TotalStats totalStats = TotalStats.newBuilder().build();
            int equipmentCount = 0;
            int totalLevel = 0;
            int totalQuality = 0;
            
            if (listResp.getItems() != null && !listResp.getItems().isEmpty()) {
                equipmentCount = listResp.getItems().size();
                totalLevel = equipmentCount;  // Default level 1 each
                totalQuality = equipmentCount;  // Default quality 1 each
                totalStats = calculateTotalStats(listResp.getItems());
            }
            
            EquipmentStatsResponse response = EquipmentStatsResponse.newBuilder()
                    .setTotalStats(totalStats)
                    .setEquipmentCount(equipmentCount)
                    .setAverageLevel(equipmentCount > 0 ? totalLevel / equipmentCount : 0)
                    .setAverageQuality(equipmentCount > 0 ? totalQuality / equipmentCount : 0)
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in GetEquipmentStats: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get equipment stats: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void batchGetEquipment(BatchGetEquipmentRequest request,
                                 StreamObserver<BatchGetEquipmentResponse> responseObserver) {
        log.debug("gRPC BatchGetEquipment: {} roleIds", request.getRoleIdsCount());
        
        try {
            BatchGetEquipmentResponse.Builder responseBuilder = BatchGetEquipmentResponse.newBuilder()
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build());
            
            for (Long roleId : request.getRoleIdsList()) {
                try {
                    EquipDTOs.ListResp listResp = equipService.list(roleId);
                    
                    EquipmentResponse.Builder equipResponse = EquipmentResponse.newBuilder()
                            .setRoleId(roleId)
                            .setStatus(ResponseStatus.newBuilder()
                                    .setCode(200)
                                    .setMessage("Success")
                                    .setSuccess(true)
                                    .build());
                    
                    if (listResp.getItems() != null) {
                        for (EquipDTOs.EquipItem item : listResp.getItems()) {
                            EquippedItem equippedItem = buildEquippedItem(item);
                            equipResponse.addItems(equippedItem);
                        }
                        
                        if (request.getIncludeStats()) {
                            TotalStats totalStats = calculateTotalStats(listResp.getItems());
                            equipResponse.setTotalStats(totalStats);
                        }
                    }
                    
                    responseBuilder.addEquipments(equipResponse.build());
                    
                } catch (Exception e) {
                    log.warn("Failed to get equipment for roleId {}: {}", roleId, e.getMessage());
                    // Continue with other roles
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in BatchGetEquipment: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to batch get equipment: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Helper methods
    
    private EquippedItem buildEquippedItem(EquipDTOs.EquipItem item) {
        EquippedItem.Builder builder = EquippedItem.newBuilder()
                .setSlotId(item.getEquipType())
                .setItemId(item.getItemId())
                .setLevel(1)  // Default level
                .setQuality(1);  // Default quality
        
        // Add attributes
        if (item.getHp() > 0) {
            builder.addAttributes(Attribute.newBuilder()
                    .setAttrType("HP")
                    .setValue(item.getHp())
                    .build());
        }
        if (item.getAttack() > 0) {
            builder.addAttributes(Attribute.newBuilder()
                    .setAttrType("ATTACK")
                    .setValue(item.getAttack())
                    .build());
        }
        if (item.getDefend() > 0) {
            builder.addAttributes(Attribute.newBuilder()
                    .setAttrType("DEFENSE")
                    .setValue(item.getDefend())
                    .build());
        }
        if (item.getSpeed() > 0) {
            builder.addAttributes(Attribute.newBuilder()
                    .setAttrType("SPEED")
                    .setValue(item.getSpeed())
                    .build());
        }
        
        return builder.build();
    }
    
    private TotalStats calculateTotalStats(List<EquipDTOs.EquipItem> equipList) {
        int totalHp = 0;
        int totalAtk = 0;
        int totalDef = 0;
        int totalSpeed = 0;
        int totalFightPower = 0;
        
        for (EquipDTOs.EquipItem item : equipList) {
            totalHp += item.getHp();
            totalAtk += item.getAttack();
            totalDef += item.getDefend();
            totalSpeed += item.getSpeed();
        }
        
        // Calculate fight power (simple formula)
        totalFightPower = totalHp / 10 + totalAtk * 3 + totalDef * 2;
        
        return TotalStats.newBuilder()
                .setHpBonus(totalHp)
                .setAttackBonus(totalAtk)
                .setDefenseBonus(totalDef)
                .setSpeedBonus(totalSpeed)
                .setCritRateBonus(0)
                .setCritDamageBonus(0)
                .setFightPowerBonus(totalFightPower)
                .build();
    }
}

