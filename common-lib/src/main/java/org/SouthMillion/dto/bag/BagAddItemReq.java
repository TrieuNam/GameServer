package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Request to add items to bag (standalone class for direct import)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagAddItemReq {
    @NotNull
    private Long userId;

    @NotNull
    private Long roleId;

    @NotEmpty
    private List<Item> items;

    private String source;

    private String idemKey;

    private Integer reason;

    private Integer reasonType;

    // Convenience constructor for common usage
    public BagAddItemReq(Long userId, Long roleId, List<Item> items, String source) {
        this.userId = userId;
        this.roleId = roleId;
        this.items = items;
        this.source = source;
    }

    public Long userId() {
        return userId;
    }

    public Long roleId() {
        return roleId;
    }

    public List<Item> items() {
        return items;
    }

    /**
     * Nested Item class for item delta
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @NotNull
        private Integer itemId;

        @NotNull
        @JsonAlias("quantity")
        private Integer amount;

        private Integer quality;

        @JsonAlias("bind")
        private Boolean bound;

        @JsonAlias("bag_type")
        private Integer bagType;

        // Convenience constructor
        public Item(Integer itemId, Integer amount) {
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

