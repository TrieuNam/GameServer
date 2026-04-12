package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.shop.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC Client Service for shop-service
 * Replaces REST Feign calls with high-performance gRPC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShopGrpcClient.class);

    @GrpcClient("shop-service")
    private ShopServiceGrpc.ShopServiceBlockingStub shopServiceStub;

    /**
     * Get shop items
     */
    public List<ShopItem> getShopItems(String shopType, int roleLevel, int vipLevel) {
        try {
            GetShopItemsRequest request = GetShopItemsRequest.newBuilder()
                    .setShopType(shopType)
                    .setRoleLevel(roleLevel)
                    .setVipLevel(vipLevel)
                    .build();

            ShopItemsResponse response = shopServiceStub.getShopItems(request);
            return response.getItemsList();

        } catch (StatusRuntimeException e) {
            log.error("[grpc-shop] Failed to get shop items: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Purchase an item
     */
    public PurchaseResponse purchase(Long roleId, int shopItemId, int quantity, String currencyType) {
        try {
            PurchaseRequest request = PurchaseRequest.newBuilder()
                    .setRoleId(roleId)
                    .setShopItemId(shopItemId)
                    .setQuantity(quantity)
                    .setCurrencyType(currencyType != null ? currencyType : "GOLD")
                    .build();

            PurchaseResponse response = shopServiceStub.purchase(request);
            
            if (response.getSuccess()) {
                log.info("[grpc-shop] Purchase successful for roleId={}, shopItemId={}, qty={}", 
                        roleId, shopItemId, quantity);
            }
            
            return response;

        } catch (StatusRuntimeException e) {
            log.error("[grpc-shop] Failed to purchase item: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Batch purchase items
     */
    public BatchPurchaseResponse batchPurchase(Long roleId, List<PurchaseItem> items) {
        try {
            BatchPurchaseRequest.Builder builder = BatchPurchaseRequest.newBuilder()
                    .setRoleId(roleId);
            
            items.forEach(builder::addItems);

            return shopServiceStub.batchPurchase(builder.build());

        } catch (StatusRuntimeException e) {
            log.error("[grpc-shop] Failed to batch purchase: {}", e.getMessage());
            return null;
        }
    }
}
