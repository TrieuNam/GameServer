package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Bag/Inventory DTOs for inter-service communication
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

    /**
     * Request to add items to bag
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddItemReq {
        /**
         * User ID
         */
        @NotNull
        private Long userId;

        /**
         * Role ID
         */
        @NotNull
        private Long roleId;

        /**
         * Items to add
         */
        @NotEmpty
        private List<ItemDelta> items;

        /**
         * Source of items (for logging/tracking)
         */
        private String source;

        /**
         * Idempotency key (optional)
         */
        private String idemKey;

        /**
         * Reason code (for logging)
         */
        private Integer reason;

        /**
         * Reason type/category
         */
        private Integer reasonType;

        // Record-style accessors
        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }

        public List<ItemDelta> items() {
            return items;
        }

        public String source() {
            return source;
        }
    }

    /**
     * Response for add item operation
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddItemResp {
        /**
         * Success flag
         */
        private Boolean success;

        /**
         * Items that were actually added
         */
        private List<ItemDelta> added;

        /**
         * Error message if failed
         */
        private String message;

        /**
         * Error code if failed
         */
        private Integer errorCode;

        // Record-style accessors
        public Boolean success() {
            return success;
        }

        public List<ItemDelta> added() {
            return added;
        }
    }

    /**
     * Request to consume items from bag
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsumeReq {
        /**
         * User ID
         */
        @NotNull
        private Long userId;

        /**
         * Role ID
         */
        @NotNull
        private Long roleId;

        /**
         * Item ID to consume
         */
        @NotNull
        private Long itemId;

        /**
         * Amount to consume
         */
        @NotNull
        private Integer amount;

        /**
         * Source/reason for consumption
         */
        private String source;

        /**
         * Idempotency key (optional)
         */
        private String idemKey;

        // Record-style accessors
        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }

        public Long itemId() {
            return itemId;
        }

        public Integer amount() {
            return amount;
        }
    }

    /**
     * Generic OK response
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OkResp {
        /**
         * Success flag
         */
        private Boolean success;

        /**
         * Message
         */
        private String message;

        /**
         * Error code if failed
         */
        private Integer errorCode;

        // Convenience constructors
        public static OkResp ok() {
            return new OkResp(true, "Success", null);
        }

        public static OkResp error(String message, Integer errorCode) {
            return new OkResp(false, message, errorCode);
        }

        // Record-style accessors
        public Boolean success() {
            return success;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Bag view/snapshot
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagView {
        /**
         * User ID
         */
        private Long userId;

        /**
         * Role ID
         */
        private Long roleId;

        /**
         * All items in bag
         */
        private List<ItemDelta> items;

        /**
         * Bag capacity
         */
        private Integer capacity;

        /**
         * Current item count
         */
        private Integer itemCount;

        // Record-style accessors
        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }

        public List<ItemDelta> items() {
            return items;
        }
    }

    /**
     * Request to get bag contents
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GetBagReq {
        /**
         * User ID
         */
        @NotNull
        private Long userId;

        /**
         * Role ID
         */
        @NotNull
        private Long roleId;

        // Record-style accessors
        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }
    }

    /**
     * Batch add items request (alternative naming)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BagAddItemReq {
        @NotNull
        private Long userId;

        @NotNull
        private Long roleId;

        @NotEmpty
        private List<ItemDelta> items;

        private String source;

        private String idemKey;

        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }

        public List<ItemDelta> items() {
            return items;
        }
    }

    /**
     * Batch add items response (alternative naming)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagAddItemResp {
        private Boolean success;
        private List<ItemDelta> added;
        private String message;

        public Boolean success() {
            return success;
        }

        public List<ItemDelta> added() {
            return added;
        }
    }

    /**
     * Consume items request (alternative naming)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BagConsumeReq {
        @NotNull
        private Long userId;

        @NotNull
        private Long roleId;

        @NotNull
        private Long itemId;

        @NotNull
        private Integer amount;

        private String source;

        public Long userId() {
            return userId;
        }

        public Long roleId() {
            return roleId;
        }

        public Long itemId() {
            return itemId;
        }

        public Integer amount() {
            return amount;
        }
    }

    /**
     * Generic OK response (alternative naming)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagOkResp {
        private Boolean success;
        private String message;
        private Integer errorCode;

        public static BagOkResp ok() {
            return new BagOkResp(true, "Success", null);
        }

        public static BagOkResp error(String message) {
            return new BagOkResp(false, message, -1);
        }

        public Boolean success() {
            return success;
        }

        public String message() {
            return message;
        }
    }
}

