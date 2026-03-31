package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Bag/Inventory DTOs for inter-service communication
 *
 * Note: Most classes moved to standalone files for easier import.
 * Use: BagAddItemReq, BagAddItemResp, BagConsumeReq, BagOkResp  
 * This file keeps inner classes for backward compatibility with service code.
 */
public class BagDTOs {

    /**
     * Item view for displaying bag items
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemView {
        private Long id;
        private String roleId;
        private Integer itemId;
        private Integer num;
        private Integer bind;
        private Instant expireAt;
        private Integer quality;
        private Integer bagType;
    }

    /**
     * Grant item to player (used in grants/events)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrantItem {
        @NotNull
        private Integer itemId;
        
        @NotNull
        @JsonAlias("quantity")
        private Integer num;
        
        private Integer quality;
        @JsonAlias("bound")
        private Integer bind;
        @JsonAlias("bag_type")
        private Integer bagType;
        private Instant expireAt;
    }

    /**
     * Request to grant items to player
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrantReq {
        private String userId;
        private String roleId;
        private List<GrantItem> items;
        private String eventId;
        private String source;
        private String reason;
    }

    /**
     * Request to use/consume an item
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UseItemReq {
        @NotNull
        private Integer itemId;
        
        @NotNull
        @JsonAlias("quantity")
        private Integer num;  // quantity
        
        private Integer param;  // optional parameter
        
        private String reason;
    }

    /**
     * Request to sell items
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellItemReq {
        @NotNull
        private Integer itemId;
        
        @NotNull
        @JsonAlias("quantity")
        private Integer num;
        
        private Long unitPrice;  // Price per item
    }

    /**
     * Result of sell operation
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellResult {
        private Long goldEarned;
        private Integer itemsSold;
    }

    /**
     * Item delta representing item ID and amount
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDelta {
        /**
         * Item ID
         */
        @NotNull
        private Integer itemId;

        /**
         * Amount (positive for add, negative for consume)
         */
        @NotNull
        private Integer amount;

        /**
         * Optional: item quality/level
         */
        private Integer quality;

        /**
         * Optional: item binding status
         */
        private Boolean bound;

        // Convenience constructor
        public ItemDelta(Integer itemId, Integer amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        // Record-style accessors
        public Integer itemId() {
            return itemId;
        }

        public Integer amount() {
            return amount;
        }
    }

    /**
     * Request for 万能卡 (all-purpose card) buy-cmd flow (MsgId 1501)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuyCmdReq {
        /** PB_CSBuyCmdReq.buy_type */
        private int buyType;
        /** item to receive — mapped from buy_param_1 */
        private int itemId;
        /** quantity to grant */
        private int num;
        /** currency/card item ID (0 = free/handled by config) */
        private int currency;
        /** card/currency cost per unit */
        private int price;
    }
}

