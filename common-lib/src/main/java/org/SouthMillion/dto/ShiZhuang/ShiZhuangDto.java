package org.SouthMillion.dto.ShiZhuang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ShiZhuang (Fashion/Costume) entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiZhuangDto {

    private int id;

    private String userId;

    /** Current costume level / unlocked tier */
    private int level;
}
