package org.SouthMillion.dto.ShiZhuang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for player angel/celestial information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerAngelDTO {

    private int angelLevel;
    private int angelGrade;
    private int usedSkinSeq;
    private List<Integer> equipIds;
    private List<AngelAppearanceDTO> appearanceDataList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AngelAppearanceDTO {
        private int id;
        private int level;
    }
}
