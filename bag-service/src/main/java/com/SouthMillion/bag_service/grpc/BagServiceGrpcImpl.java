package com.SouthMillion.bag_service.grpc;

import com.SouthMillion.bag_service.service.BagDomainService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.grpc.bag.*;
import org.SouthMillion.grpc.common.ItemStack;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Service Implementation for Bag/Inventory System
 * High-performance inventory operations via gRPC
 * 
 * Target Performance: <15ms per operation
 * Throughput: >3000 operations/sec
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BagServiceGrpcImpl extends BagServiceGrpc.BagServiceImplBase {

    private final BagDomainService bagDomainService;

        @Value("${bag.maxSlots:200}")
        private int maxSlots;

    @Override
    public void getInventory(GetInventoryRequest request,
                            StreamObserver<InventoryResponse> responseObserver) {
        log.debug("gRPC GetInventory: roleId={}", request.getRoleId());
        try {
            Long roleId = request.getRoleId();
            List<BagDTOs.ItemView> items = bagDomainService.list(roleId);
            
            InventoryResponse.Builder responseBuilder = InventoryResponse.newBuilder()
                    .setRoleId(request.getRoleId())
                    .setUsedSlots(items.size())
                    .setMaxSlots(maxSlots)
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build());
            
            // Convert items to inventory slots
            for (int i = 0; i < items.size(); i++) {
                BagDTOs.ItemView item = items.get(i);
                InventorySlot slot = InventorySlot.newBuilder()
                        .setSlotId(i + 1)
                        .setItemId(item.getItemId())
                        .setQuantity(item.getNum() != null ? item.getNum().intValue() : 0)
                        .setIsBound(item.getBind() != null && item.getBind() != 0)
                        .setExpireTime(item.getExpireAt() != null ? item.getExpireAt().toEpochMilli() : 0)
                        .build();
                responseBuilder.addSlots(slot);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in GetInventory: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get inventory: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void addItems(AddItemsRequest request,
                        StreamObserver<AddItemsResponse> responseObserver) {
        log.debug("gRPC AddItems: roleId={}, items={}", request.getRoleId(), request.getItemsCount());
        try {
            Long roleId = request.getRoleId();
            String userId = String.valueOf(request.getRoleId()); // Fallback: use roleId as userId
            String eventId = request.getEventId().isEmpty() ? 
                    java.util.UUID.randomUUID().toString() : request.getEventId();
            
            // Convert proto items to DTOs
            List<BagDTOs.GrantItem> grantItems = request.getItemsList().stream()
                    .map(item -> BagDTOs.GrantItem.builder()
                            .itemId(item.getItemId())
                            .num(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            // Grant items
            List<BagDTOs.ItemView> addedItems = bagDomainService.grant(
                    userId, roleId, grantItems, eventId);
            
            AddItemsResponse.Builder responseBuilder = AddItemsResponse.newBuilder()
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Items added successfully")
                            .setSuccess(true)
                            .build());
            
            // Add slots
            addedItems.forEach(item -> {
                InventorySlot slot = InventorySlot.newBuilder()
                        .setSlotId(0)  // Auto-assigned
                        .setItemId(item.getItemId())
                                                .setQuantity(item.getNum() != null ? item.getNum().intValue() : 0)
                                                .setIsBound(item.getBind() != null && item.getBind() != 0)
                                                .setExpireTime(item.getExpireAt() != null ? item.getExpireAt().toEpochMilli() : 0)
                        .build();
                responseBuilder.addAddedSlots(slot);
            });
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in AddItems: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to add items: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void removeItems(RemoveItemsRequest request,
                           StreamObserver<RemoveItemsResponse> responseObserver) {
        log.debug("gRPC RemoveItems: roleId={}, items={}", request.getRoleId(), request.getItemsCount());
        try {
            Long roleId = request.getRoleId();
            
            // Convert proto items to DTOs
            List<BagDTOs.GrantItem> removeItems = request.getItemsList().stream()
                    .map(item -> BagDTOs.GrantItem.builder()
                            .itemId(item.getItemId())
                            .num(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            // Remove items using bag domain service
            for (BagDTOs.GrantItem item : removeItems) {
                BagDTOs.UseItemReq useReq = BagDTOs.UseItemReq.builder()
                        .itemId(item.getItemId())
                        .num(item.getNum() != null ? item.getNum() : 1)
                        .reason(request.getReason())
                        .build();
                bagDomainService.use(roleId, useReq);
            }
            
            RemoveItemsResponse response = RemoveItemsResponse.newBuilder()
                    .setSuccess(true)
                    .addAllRemovedItemIds(request.getItemsList().stream()
                            .map(ItemStack::getItemId)
                            .collect(Collectors.toList()))
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Items removed successfully")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in RemoveItems: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to remove items: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void useItem(UseItemRequest request,
                       StreamObserver<UseItemResponse> responseObserver) {
        log.debug("gRPC UseItem: roleId={}, itemId={}", request.getRoleId(), request.getItemId());
        try {
            Long roleId = request.getRoleId();
            BagDTOs.UseItemReq useReq = BagDTOs.UseItemReq.builder()
                    .itemId(request.getItemId())
                    .num(request.getQuantity())
                    .reason("USE_ITEM")
                    .build();
            bagDomainService.use(roleId, useReq);
            int remaining = bagDomainService.list(roleId).stream()
                    .filter(item -> item.getItemId() != null && item.getItemId().equals(request.getItemId()))
                    .mapToInt(item -> item.getNum() != null ? item.getNum().intValue() : 0)
                    .sum();
            
            UseItemResponse response = UseItemResponse.newBuilder()
                    .setSuccess(true)
                    .setRemainingQuantity(remaining)
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Item used successfully")
                            .setSuccess(true)
                            .build())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in UseItem: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to use item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void hasItems(HasItemsRequest request,
                        StreamObserver<HasItemsResponse> responseObserver) {
        log.debug("gRPC HasItems: roleId={}, items={}", request.getRoleId(), request.getRequiredItemsCount());
        try {
            Long roleId = request.getRoleId();
            List<BagDTOs.ItemView> inventory = bagDomainService.list(roleId);
            
            // Check each item
            HasItemsResponse.Builder responseBuilder = HasItemsResponse.newBuilder()
                    .setHasAll(true)
                    .setStatus(ResponseStatus.newBuilder()
                            .setCode(200)
                            .setMessage("Success")
                            .setSuccess(true)
                            .build());
            
            for (ItemStack requestItem : request.getRequiredItemsList()) {
                int availableQuantity = inventory.stream()
                        .filter(item -> item.getItemId() != null && item.getItemId().equals(requestItem.getItemId()))
                                                .mapToInt(item -> item.getNum() != null ? item.getNum().intValue() : 0)
                        .sum();
                boolean hasEnough = availableQuantity >= requestItem.getQuantity();
                
                responseBuilder.addItemStatus(ItemAvailability.newBuilder()
                        .setItemId(requestItem.getItemId())
                        .setRequiredQuantity(requestItem.getQuantity())
                        .setAvailableQuantity(availableQuantity)
                        .setHasEnough(hasEnough)
                        .build());
                
                if (!hasEnough) {
                    responseBuilder.setHasAll(false);
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in HasItems: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to check items: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void streamInventoryUpdates(InventoryStreamRequest request,
                                      StreamObserver<InventoryUpdateEvent> responseObserver) {
        log.debug("gRPC StreamInventoryUpdates: roleId={}", request.getRoleId());
        try {
            Long roleId = request.getRoleId();
            List<BagDTOs.ItemView> items = bagDomainService.list(roleId);

            InventoryUpdateEvent.Builder updateBuilder = InventoryUpdateEvent.newBuilder()
                            .setRoleId(request.getRoleId())
                            .setEventType("SNAPSHOT")
                            .setTimestamp(System.currentTimeMillis());

            for (int i = 0; i < items.size(); i++) {
                    BagDTOs.ItemView item = items.get(i);
                    InventorySlot slot = InventorySlot.newBuilder()
                                    .setSlotId(i + 1)
                                    .setItemId(item.getItemId())
                                    .setQuantity(item.getNum() != null ? item.getNum().intValue() : 0)
                                    .setIsBound(item.getBind() != null && item.getBind() != 0)
                                    .setExpireTime(item.getExpireAt() != null ? item.getExpireAt().toEpochMilli() : 0)
                                    .build();
                    updateBuilder.addAffectedSlots(slot);
            }

            responseObserver.onNext(updateBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in StreamInventoryUpdates: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}

