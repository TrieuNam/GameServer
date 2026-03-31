package com.SouthMillion.crafting_service.service;

import com.SouthMillion.crafting_service.client.BagGrpcClient;
import com.SouthMillion.crafting_service.client.WalletGrpcClient;
import com.SouthMillion.crafting_service.entity.CraftingRecipe;
import com.SouthMillion.crafting_service.entity.UserCrafting;
import com.SouthMillion.crafting_service.repository.CraftingRecipeRepository;
import com.SouthMillion.crafting_service.repository.UserCraftingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.crafting.CraftingDTOs;
import org.SouthMillion.grpc.common.ItemStack;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business logic cho crafting.
 *
 * Chiến lược giao tiếp với các service khác — TẤT CẢ dùng gRPC:
 *  - bag-service    → gRPC (BagGrpcClient)    — port 9230
 *  - wallet-service → gRPC (WalletGrpcClient) — port 9210
 *
 * REST chỉ dùng cho external/admin HTTP endpoints (CraftingController).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CraftingService {

    private final CraftingRecipeRepository recipeRepo;
    private final UserCraftingRepository userCraftingRepo;
    /** gRPC — bag-service (kiểm tra, tiêu hao nguyên liệu, trao thưởng item) */
    private final BagGrpcClient bagGrpcClient;
    /** gRPC — wallet-service (trừ/cộng coin) */
    private final WalletGrpcClient walletGrpcClient;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // GET RECIPES
    // ─────────────────────────────────────────────────────────────

    /**
     * Trả danh sách công thức. Với mỗi nguyên liệu, điền currentAmount từ bag qua gRPC.
     */
    public List<CraftingDTOs.RecipeInfo> getRecipes(String roleId, Integer levelFilter) {
        List<CraftingRecipe> recipes = levelFilter != null
                ? recipeRepo.findByRequiredLevelLessThanEqualAndEnabledTrue(levelFilter)
                : recipeRepo.findByEnabledTrue();

        // Lấy số lượng items trong túi qua gRPC (một lần duy nhất)
        Map<Integer, Integer> bagCounts = Collections.emptyMap();
        Long rid = parseLongSafe(roleId);
        if (rid != null && rid > 0) {
            bagCounts = bagGrpcClient.getItemCounts(rid);   // gRPC call
        }

        final Map<Integer, Integer> counts = bagCounts;
        return recipes.stream()
                .map(r -> toRecipeInfo(r, counts))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // START CRAFT
    // ─────────────────────────────────────────────────────────────

    /**
     * Bắt đầu chế tác:
     *  1. Kiểm tra công thức
     *  2. Kiểm tra nguyên liệu đủ  (gRPC → bag-service)
     *  3. Kiểm tra và trừ coin     (REST → wallet-service)
     *  4. Tiêu hao nguyên liệu     (gRPC → bag-service)
     *  5. Tạo bản ghi UserCrafting
     */
    @Transactional
    public CraftingDTOs.CraftResponse startCraft(CraftingDTOs.CraftRequest request) {
        CraftingRecipe recipe = recipeRepo.findByRecipeId(request.getRecipeId()).orElse(null);
        if (recipe == null || !Boolean.TRUE.equals(recipe.getEnabled())) {
            return CraftingDTOs.CraftResponse.builder()
                    .success(false).message("Recipe not found or disabled").build();
        }

        Long roleId = parseLongSafe(request.getRoleId());
        if (roleId == null) {
            return CraftingDTOs.CraftResponse.builder()
                    .success(false).message("Invalid roleId").build();
        }

        List<CraftingDTOs.Material> materials = parseMaterials(recipe.getMaterialsJson());

        // ── 1. Kiểm tra đủ nguyên liệu qua gRPC ──
        if (!materials.isEmpty()) {
            List<ItemStack> required = toItemStacks(materials);
            if (!bagGrpcClient.hasItems(roleId, required)) {    // gRPC call
                return CraftingDTOs.CraftResponse.builder()
                        .success(false).message("Not enough materials").build();
            }
        }

        // ── 2. Trừ coin qua gRPC (wallet-service) ──
        if (recipe.getCoinCost() != null && recipe.getCoinCost() > 0) {
            boolean deducted = walletGrpcClient.deductCoin(roleId, 1L, recipe.getCoinCost(), 20);  // gRPC call, reason 20 = CRAFT
            if (!deducted) {
                return CraftingDTOs.CraftResponse.builder()
                        .success(false).message("Not enough coins").build();
            }
        }

        // ── 3. Tiêu hao nguyên liệu qua gRPC ──
        if (!materials.isEmpty()) {
            boolean removed = bagGrpcClient.removeItems(roleId, toItemStacks(materials), "CRAFT"); // gRPC call
            if (!removed) {
                log.error("[crafting] removeItems failed roleId={} recipeId={}", roleId, recipe.getRecipeId());
                return CraftingDTOs.CraftResponse.builder()
                        .success(false).message("Failed to consume materials").build();
            }
        }

        // ── 4. Tạo bản ghi chế tác ──
        long now = Instant.now().getEpochSecond();
        long endEpoch = now + (recipe.getCraftTime() != null ? recipe.getCraftTime() : 0L);

        UserCrafting uc = new UserCrafting();
        uc.setRoleId(request.getRoleId());
        uc.setRecipeId(recipe.getRecipeId());
        uc.setStartTime(Instant.ofEpochSecond(now));
        uc.setEndTime(Instant.ofEpochSecond(endEpoch));
        uc.setStatus("IN_PROGRESS");
        userCraftingRepo.save(uc);

        log.info("[crafting] startCraft OK roleId={} recipeId={} craftingId={}", roleId, recipe.getRecipeId(), uc.getId());
        return CraftingDTOs.CraftResponse.builder()
                .success(true).message("Crafting started")
                .craftingId(uc.getId()).endTime(endEpoch)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // GET STATUS
    // ─────────────────────────────────────────────────────────────

    public List<CraftingDTOs.CraftingStatus> getStatus(String roleId) {
        Instant now = Instant.now();
        return userCraftingRepo.findByRoleId(roleId).stream()
                .map(uc -> {
                    boolean done = now.isAfter(uc.getEndTime());
                    return CraftingDTOs.CraftingStatus.builder()
                            .craftingId(uc.getId())
                            .recipeId(uc.getRecipeId())
                            .status(uc.getStatus())
                            .startTime(uc.getStartTime().getEpochSecond())
                            .endTime(uc.getEndTime().getEpochSecond())
                            .canClaim(done && "IN_PROGRESS".equals(uc.getStatus()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // CANCEL
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public CraftingDTOs.CancelResponse cancel(CraftingDTOs.CancelRequest request) {
        UserCrafting uc = userCraftingRepo.findById(request.getCraftingId()).orElse(null);
        if (uc == null || !uc.getRoleId().equals(request.getRoleId())) {
            return CraftingDTOs.CancelResponse.builder()
                    .success(false).message("Crafting record not found").build();
        }
        if (!"IN_PROGRESS".equals(uc.getStatus())) {
            return CraftingDTOs.CancelResponse.builder()
                    .success(false).message("Cannot cancel: status is " + uc.getStatus()).build();
        }
        uc.setStatus("CANCELLED");
        userCraftingRepo.save(uc);
        log.info("[crafting] Cancelled craftingId={} for roleId={}", uc.getId(), uc.getRoleId());
        return CraftingDTOs.CancelResponse.builder()
                .success(true).message("Crafting cancelled").craftingId(uc.getId()).build();
    }

    // ─────────────────────────────────────────────────────────────
    // CLAIM
    // ─────────────────────────────────────────────────────────────

    /**
     * Nhận thưởng khi chế tác hoàn thành:
     *  1. Kiểm tra trạng thái và thời gian
     *  2. Cộng item vào túi qua gRPC (bag-service)
     *  3. Cập nhật trạng thái → CLAIMED
     */
    @Transactional
    public CraftingDTOs.ClaimResponse claim(CraftingDTOs.ClaimRequest request) {
        UserCrafting uc = userCraftingRepo.findById(request.getCraftingId()).orElse(null);
        if (uc == null) {
            return CraftingDTOs.ClaimResponse.builder()
                    .success(false).message("Crafting not found").build();
        }
        if (!uc.getRoleId().equals(request.getRoleId())) {
            return CraftingDTOs.ClaimResponse.builder()
                    .success(false).message("Not your crafting").build();
        }
        if (!"IN_PROGRESS".equals(uc.getStatus())) {
            return CraftingDTOs.ClaimResponse.builder()
                    .success(false).message("Already claimed or invalid status").build();
        }
        if (Instant.now().isBefore(uc.getEndTime())) {
            return CraftingDTOs.ClaimResponse.builder()
                    .success(false).message("Crafting not completed yet").build();
        }

        CraftingRecipe recipe = recipeRepo.findByRecipeId(uc.getRecipeId()).orElse(null);
        List<CraftingDTOs.RewardItem> rewards = Collections.emptyList();

        if (recipe != null) {
            rewards = List.of(CraftingDTOs.RewardItem.builder()
                    .itemId(recipe.getResultItemId())
                    .amount(recipe.getResultAmount())
                    .build());

            // Cộng item vào túi qua gRPC
            Long roleId = parseLongSafe(request.getRoleId());
            if (roleId != null) {
                List<ItemStack> rewardStacks = rewards.stream()
                        .map(r -> ItemStack.newBuilder()
                                .setItemId(r.getItemId())
                                .setQuantity(r.getAmount())
                                .build())
                        .collect(Collectors.toList());
                boolean added = bagGrpcClient.addItems(roleId, rewardStacks, "CRAFT");  // gRPC call
                if (!added) {
                    log.error("[crafting] addItems failed roleId={} craftingId={}", roleId, uc.getId());
                    return CraftingDTOs.ClaimResponse.builder()
                            .success(false).message("Failed to grant reward items").build();
                }
            }
        }

        uc.setStatus("CLAIMED");
        userCraftingRepo.save(uc);

        log.info("[crafting] claim OK roleId={} craftingId={}", request.getRoleId(), uc.getId());
        return CraftingDTOs.ClaimResponse.builder()
                .success(true).message("Claimed successfully").rewards(rewards).build();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private CraftingDTOs.RecipeInfo toRecipeInfo(CraftingRecipe r, Map<Integer, Integer> bagCounts) {
        List<CraftingDTOs.Material> rawMaterials = parseMaterials(r.getMaterialsJson());
        // Điền currentAmount từ bag (đã lấy qua gRPC một lần trong getRecipes)
        List<CraftingDTOs.Material> materials = rawMaterials.stream()
                .map(m -> CraftingDTOs.Material.builder()
                        .itemId(m.getItemId())
                        .amount(m.getAmount())
                        .currentAmount(bagCounts.getOrDefault(m.getItemId(), 0))
                        .build())
                .collect(Collectors.toList());
        // canCraft = tất cả nguyên liệu đủ số lượng
        boolean canCraft = materials.stream()
                .allMatch(m -> m.getCurrentAmount() >= (m.getAmount() != null ? m.getAmount() : 0));

        return CraftingDTOs.RecipeInfo.builder()
                .recipeId(r.getRecipeId())
                .recipeName(r.getRecipeName())
                .resultItemId(r.getResultItemId())
                .resultAmount(r.getResultAmount())
                .materials(materials)
                .craftTime(r.getCraftTime())
                .requiredLevel(r.getRequiredLevel())
                .coinCost(r.getCoinCost())
                .canCraft(canCraft)
                .build();
    }

    /** Chuyển danh sách Material DTO → gRPC ItemStack list */
    private List<ItemStack> toItemStacks(List<CraftingDTOs.Material> materials) {
        return materials.stream()
                .filter(m -> m.getItemId() != null && m.getAmount() != null && m.getAmount() > 0)
                .map(m -> ItemStack.newBuilder()
                        .setItemId(m.getItemId())
                        .setQuantity(m.getAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CraftingDTOs.Material> parseMaterials(String json) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<CraftingDTOs.Material>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse materials json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long parseLongSafe(String s) {
        try {
            return s == null ? null : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
