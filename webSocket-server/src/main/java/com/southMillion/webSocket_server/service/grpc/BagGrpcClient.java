package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.grpc.bag.*;
import org.SouthMillion.grpc.common.ItemStack;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC Client Service for bag-service
 * Replaces REST Feign calls with high-performance gRPC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BagGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BagGrpcClient.class);

    @GrpcClient("bag-service")
    private BagServiceGrpc.BagServiceBlockingStub bagServiceStub;

    /**
     * Get inventory items for a role
     */
    public List<BagDTOs.ItemView> list(Long roleId) {
        try {
            GetInventoryRequest request = GetInventoryRequest.newBuilder()
                    .setRoleId(roleId)
                    .build();

            InventoryResponse response = bagServiceStub.getInventory(request);

            return response.getSlotsList().stream()
                    .map(slot -> BagDTOs.ItemView.builder()
                            .itemId(slot.getItemId())
                            .num(slot.getQuantity())
                            .build())
                    .collect(Collectors.toList());

        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] Failed to get inventory for roleId={}: {}", roleId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Grant items to a role
     */
    public List<BagDTOs.ItemView> grant(BagDTOs.GrantReq req) {
        try {
            AddItemsRequest.Builder requestBuilder = AddItemsRequest.newBuilder()
                    .setRoleId(Long.parseLong(req.getRoleId()))
                    .setSource(req.getSource() != null ? req.getSource() : "grant");

            if (req.getItems() != null) {
                req.getItems().forEach(item -> {
                    ItemStack itemStack = ItemStack.newBuilder()
                            .setItemId(item.getItemId().intValue())
                            .setQuantity(item.getNum() != null ? item.getNum() : 1)
                            .build();
                    requestBuilder.addItems(itemStack);
                });
            }

            AddItemsResponse response = bagServiceStub.addItems(requestBuilder.build());

            // Convert response slots back to ItemView
            return response.getAddedSlotsList().stream()
                    .map(slot -> BagDTOs.ItemView.builder()
                            .itemId(slot.getItemId())
                            .num(slot.getQuantity())
                            .build())
                    .collect(Collectors.toList());

        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] Failed to grant items: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Use an item
     */
    public void use(Long roleId, BagDTOs.UseItemReq req) {
        try {
            UseItemRequest request = UseItemRequest.newBuilder()
                    .setRoleId(roleId)
                    .setItemId(req.getItemId().intValue())
                    .setQuantity(req.getNum() != null ? req.getNum() : 1)
                    .build();

            UseItemResponse response = bagServiceStub.useItem(request);

            if (!response.getSuccess()) {
                log.warn("[grpc-bag] Use item failed");
            }

        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] Failed to use item: {}", e.getMessage());
        }
    }

    /**
     * Check if role has specific items
     */
    public boolean hasItems(Long roleId, long itemId, int count) {
        try {
            HasItemsRequest request = HasItemsRequest.newBuilder()
                    .setRoleId(roleId)
                    .addRequiredItems(ItemStack.newBuilder()
                            .setItemId((int) itemId)
                            .setQuantity(count)
                            .build())
                    .build();

            HasItemsResponse response = bagServiceStub.hasItems(request);
            return response.getHasAll();

        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] Failed to check items: {}", e.getMessage());
            return false;
        }
    }
}
