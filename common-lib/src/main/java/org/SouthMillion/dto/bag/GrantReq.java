package org.SouthMillion.dto.bag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * Request to grant items to player (GM/event/quest)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantReq {
    @NotNull
    private Long userId;
    
    @NotBlank
    private String roleId;
    
    @NotEmpty
    private List<ItemInfo> items;
    
    private String eventId;
    
    private String source;
}
