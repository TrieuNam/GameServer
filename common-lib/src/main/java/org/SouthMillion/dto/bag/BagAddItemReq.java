package org.SouthMillion.dto.bag;

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
    private List<BagDTOs.ItemDelta> items;

    private String source;

    private String idemKey;

    public Long userId() {
        return userId;
    }

    public Long roleId() {
        return roleId;
    }

    public List<BagDTOs.ItemDelta> items() {
        return items;
    }
}

