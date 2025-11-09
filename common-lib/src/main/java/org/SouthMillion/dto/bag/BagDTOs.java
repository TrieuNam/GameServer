package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Bag/Inventory DTOs for inter-service communication
 *
 * Note: Most classes moved to standalone files for easier import.
 * Use: BagAddItemReq, BagAddItemResp, BagConsumeReq, BagOkResp
 * This file keeps only ItemDelta for backward compatibility.
 */
public class BagDTOs {

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
}

