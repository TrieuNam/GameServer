package org.SouthMillion.dto.main_fb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResp {
    private Integer index;
    private String title;
    private String desc;
    private Integer stage;
    private Integer level;
    private Integer requireScore;
    private List<RewardItem> rewards;
    private Boolean allDone;
}
