package org.SouthMillion.dto.ShiZhuang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for player_clothes join table — which clothes a player owns and at what level
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerClothesDTO {

    private Long id;

    private String playerId;

    private Integer clothesId;

    private Integer level;

    private Boolean wearing;
}
