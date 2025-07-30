package org.SouthMillion.dto.item.Box;

import lombok.Data;

@Data
public class BoxInfoDTO {
    private Integer boxLevel;
    private Integer buyTimes;
    private Long timestamp;
    private Integer arenaItemNum;
    private Integer shiZhuangNum;
    private Integer levelFetchFlag;
}
