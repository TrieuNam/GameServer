package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.NotNull;
import lombok.*;

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

