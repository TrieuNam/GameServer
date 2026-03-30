package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Request to consume items from bag (standalone class for direct import)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagConsumeReq {
    @NotNull
    private Long userId;

    @NotNull
    private Long roleId;

    @NotNull
    private Integer itemId;

    @NotNull
    @JsonAlias("quantity")
    private Integer amount;

    private String source;

    private String idemKey;

    private List<Cost> costs; // For batch consume

    // Convenience constructor for single item
    public BagConsumeReq(Long userId, Long roleId, Integer itemId, Integer amount, String source) {
        this.userId = userId;
        this.roleId = roleId;
        this.itemId = itemId;
        this.amount = amount;
        this.source = source;
    }

    // Record-style accessors
    public Long userId() {
        return userId;
    }

    public Long roleId() {
        return roleId;
    }

    public Integer itemId() {
        return itemId;
    }

    public Integer amount() {
        return amount;
    }

    /**
     * Nested Cost class for item consumption cost
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Cost {
        @NotNull
        private Integer itemId;

        @NotNull
        @JsonAlias("quantity")
        private Integer amount;

        // Record-style accessors
        public Integer itemId() {
            return itemId;
        }

        public Integer amount() {
            return amount;
        }
    }
}

