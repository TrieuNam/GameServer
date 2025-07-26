package org.SouthMillion.dto.gameworld;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainFbEvent {
    private Long userId;
    private Integer level;
    private Integer stage;
    private String type; // "LEVEL_UP", "REWARD"
}