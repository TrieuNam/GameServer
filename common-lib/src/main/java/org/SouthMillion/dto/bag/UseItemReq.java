package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request to use/consume item from bag
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UseItemReq {
    @NotNull
    private Integer itemId;
    
    @NotNull
    private Integer quantity;
    
    private String source;
}
