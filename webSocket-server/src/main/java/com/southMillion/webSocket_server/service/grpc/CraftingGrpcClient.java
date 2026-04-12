package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.crafting.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC Client Service for crafting-service.
 * Chỉ xử lý giao tiếp gRPC — không fallback REST.
 * Nếu cần REST, inject trực tiếp CraftingFeign tại nơi sử dụng.
 */
@Slf4j
@Service
public class CraftingGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CraftingGrpcClient.class);

    @GrpcClient("crafting-service")
    private CraftingServiceGrpc.CraftingServiceBlockingStub craftingServiceStub;

    // ─────────────────────────────────────────────────────────────
    // 1. GET RECIPES
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách công thức.
     * gRPC: CraftingService.getRecipes(GetRecipesRequest)
     */
    public List<Recipe> getRecipes(Long roleId, int levelFilter) {
        try {
            GetRecipesRequest request = GetRecipesRequest.newBuilder()
                    .setRoleId(roleId)
                    .setLevelFilter(levelFilter)
                    .build();
            RecipesResponse response = craftingServiceStub.getRecipes(request);
            if (response.getStatus().getSuccess()) {
                log.info("[grpc-crafting] getRecipes OK roleId={} count={}", roleId, response.getRecipesCount());
                return response.getRecipesList();
            }
            log.warn("[grpc-crafting] getRecipes failed: {}", response.getStatus().getMessage());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-crafting] getRecipes error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. START CRAFT
    // ─────────────────────────────────────────────────────────────

    /**
     * Bắt đầu chế tác.
     * gRPC: CraftingService.startCraft(StartCraftRequest)
     */
    public StartCraftResponse startCraft(Long roleId, int recipeId, int count) {
        try {
            StartCraftRequest request = StartCraftRequest.newBuilder()
                    .setRoleId(roleId)
                    .setRecipeId(recipeId)
                    .setCount(count)
                    .build();
            StartCraftResponse response = craftingServiceStub.startCraft(request);
            log.info("[grpc-crafting] startCraft OK roleId={} recipeId={} success={}",
                    roleId, recipeId, response.getSuccess());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("[grpc-crafting] startCraft error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
            return StartCraftResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("gRPC error: " + e.getStatus().getCode())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. GET STATUS
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy trạng thái chế tác đang chạy.
     * gRPC: CraftingService.getCraftingStatus(GetCraftingStatusRequest)
     */
    public List<CraftingStatus> getCraftingStatus(Long roleId) {
        try {
            GetCraftingStatusRequest request = GetCraftingStatusRequest.newBuilder()
                    .setRoleId(roleId)
                    .build();
            CraftingStatusResponse response = craftingServiceStub.getCraftingStatus(request);
            if (response.getStatus().getSuccess()) {
                log.info("[grpc-crafting] getCraftingStatus OK roleId={} count={}", roleId, response.getCraftingListCount());
                return response.getCraftingListList();
            }
            log.warn("[grpc-crafting] getCraftingStatus failed: {}", response.getStatus().getMessage());
        } catch (StatusRuntimeException e) {
            log.error("[grpc-crafting] getCraftingStatus error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
        }
        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────
    // 4. CLAIM
    // ─────────────────────────────────────────────────────────────

    /**
     * Nhận vật phẩm khi hoàn thành chế tác.
     * gRPC: CraftingService.claimCrafting(ClaimCraftingRequest)
     */
    public ClaimCraftingResponse claimCrafting(Long roleId, long craftingId) {
        try {
            ClaimCraftingRequest request = ClaimCraftingRequest.newBuilder()
                    .setRoleId(roleId)
                    .setCraftingId(craftingId)
                    .build();
            ClaimCraftingResponse response = craftingServiceStub.claimCrafting(request);
            log.info("[grpc-crafting] claimCrafting OK roleId={} craftingId={} success={}",
                    roleId, craftingId, response.getSuccess());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("[grpc-crafting] claimCrafting error status={} roleId={}: {}",
                    e.getStatus().getCode(), roleId, e.getMessage());
            return ClaimCraftingResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("gRPC error: " + e.getStatus().getCode())
                    .build();
        }
    }
}
