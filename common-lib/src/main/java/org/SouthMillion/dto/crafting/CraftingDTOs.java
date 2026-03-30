package org.SouthMillion.dto.crafting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CraftingDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecipeInfo {
        private Integer recipeId;
        private String recipeName;
        private Integer resultItemId;
        private Integer resultAmount;
        private List<Material> materials;
        private Long craftTime;
        private Integer requiredLevel;
        private Long coinCost;
        private Boolean canCraft;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Material {
        private Integer itemId;
        private Integer amount;
        /** Số lượng player đang có trong túi (do CraftingService điền qua gRPC). */
        private Integer currentAmount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CraftRequest {
        private String roleId;
        private Integer recipeId;
        private Integer count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CraftResponse {
        private Boolean success;
        private String message;
        private Long craftingId;
        private Long endTime;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CraftingStatus {
        private Long craftingId;
        private Integer recipeId;
        private String status;
        private Long startTime;
        private Long endTime;
        private Boolean canClaim;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClaimRequest {
        private String roleId;
        private Long craftingId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClaimResponse {
        private Boolean success;
        private String message;
        private List<RewardItem> rewards;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CancelRequest {
        private String roleId;
        private Long craftingId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CancelResponse {
        private Boolean success;
        private String message;
        private Long craftingId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RewardItem {
        private Integer itemId;
        private Integer amount;
    }
}
