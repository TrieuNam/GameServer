package com.SouthMillion.crafting_service.client;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.bag.*;
import org.SouthMillion.grpc.common.ItemStack;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * gRPC Client để gọi bag-service.
 *
 * Dùng gRPC (không dùng REST/Feign) vì bag-service đã có gRPC server (port 9230).
 * - getItemCounts   → BagService.GetInventory
 * - addItems        → BagService.AddItems   (trao thưởng sau claim)
 * - removeItems     → BagService.RemoveItems (tiêu hao nguyên liệu khi startCraft)
 * - hasItems        → BagService.HasItems   (kiểm tra đủ nguyên liệu)
 */
@Slf4j
@Service
public class BagGrpcClient {

    @GrpcClient("bag-service")
    private BagServiceGrpc.BagServiceBlockingStub bagServiceStub;

    // ─────────────────────────────────────────────────────────────
    // GET ITEM COUNTS  (dùng trong getRecipes để hiển thị currentAmount)
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ items trong túi của người chơi.
     *
     * @return Map<itemId, quantity>. Rỗng nếu lỗi.
     */
    public Map<Integer, Integer> getItemCounts(long roleId) {
        try {
            GetInventoryRequest request = GetInventoryRequest.newBuilder()
                    .setRoleId(roleId)
                    .setCategory("ALL")
                    .build();
            InventoryResponse response = bagServiceStub.getInventory(request);
            if (response.getStatus().getSuccess()) {
                log.debug("[grpc-bag] getInventory OK roleId={} slots={}", roleId, response.getSlotsCount());
                return response.getSlotsList().stream()
                        .collect(Collectors.toMap(
                                InventorySlot::getItemId,
                                InventorySlot::getQuantity,
                                Integer::sum));
            }
            log.warn("[grpc-bag] getInventory failed roleId={}: {}", roleId, response.getStatus().getMessage());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] getInventory error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return Collections.emptyMap();
    }

    // ─────────────────────────────────────────────────────────────
    // ADD ITEMS  (trao thưởng khi claim crafting)
    // ─────────────────────────────────────────────────────────────

    /**
     * Thêm items vào túi người chơi.
     *
     * @param source nguồn gốc item (e.g. "CRAFT")
     * @return true nếu thành công
     */
    public boolean addItems(long roleId, List<ItemStack> items, String source) {
        try {
            AddItemsRequest request = AddItemsRequest.newBuilder()
                    .setRoleId(roleId)
                    .addAllItems(items)
                    .setSource(source)
                    .build();
            AddItemsResponse response = bagServiceStub.addItems(request);
            if (response.getStatus().getSuccess()) {
                log.info("[grpc-bag] addItems OK roleId={} count={}", roleId, items.size());
                return true;
            }
            log.warn("[grpc-bag] addItems failed roleId={}: {}", roleId, response.getStatus().getMessage());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] addItems error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // REMOVE ITEMS  (tiêu hao nguyên liệu khi bắt đầu chế tác)
    // ─────────────────────────────────────────────────────────────

    /**
     * Xóa (tiêu hao) items khỏi túi người chơi.
     *
     * @param reason lý do xóa (e.g. "CRAFT")
     * @return true nếu thành công
     */
    public boolean removeItems(long roleId, List<ItemStack> items, String reason) {
        try {
            RemoveItemsRequest request = RemoveItemsRequest.newBuilder()
                    .setRoleId(roleId)
                    .addAllItems(items)
                    .setReason(reason)
                    .build();
            RemoveItemsResponse response = bagServiceStub.removeItems(request);
            if (response.getSuccess()) {
                log.info("[grpc-bag] removeItems OK roleId={} count={}", roleId, items.size());
                return true;
            }
            log.warn("[grpc-bag] removeItems failed roleId={}: {}", roleId, response.getStatus().getMessage());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] removeItems error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // HAS ITEMS  (kiểm tra đủ nguyên liệu trước khi chế tác)
    // ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra người chơi có đủ tất cả items yêu cầu không.
     *
     * @return true nếu có đủ tất cả
     */
    public boolean hasItems(long roleId, List<ItemStack> requiredItems) {
        try {
            HasItemsRequest request = HasItemsRequest.newBuilder()
                    .setRoleId(roleId)
                    .addAllRequiredItems(requiredItems)
                    .build();
            HasItemsResponse response = bagServiceStub.hasItems(request);
            log.debug("[grpc-bag] hasItems roleId={} hasAll={}", roleId, response.getHasAll());
            return response.getHasAll();
        } catch (StatusRuntimeException e) {
            log.error("[grpc-bag] hasItems error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return false;
    }
}

