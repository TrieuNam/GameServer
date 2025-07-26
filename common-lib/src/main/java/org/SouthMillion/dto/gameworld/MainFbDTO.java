package org.SouthMillion.dto.gameworld;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.proto.Msgmainfb.Msgmainfb;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainFbDTO {
    private Long userId;
    private Integer level;
    private Integer stage;
    private Integer diaFetchNum;
    private Long lastFetchTime;

    // Mapping sang proto (ví dụ tên package: Msgmainfb)
    public Msgmainfb.PB_SCMainFbInfo toProto() {
        return Msgmainfb.PB_SCMainFbInfo.newBuilder()
                .setLevel(level == null ? 0 : level)
                .setStage(stage == null ? 0 : stage)
                .setDiaFetchNum(diaFetchNum == null ? 0 : diaFetchNum)
                .setLastFetchTime(lastFetchTime == null ? 0 : lastFetchTime.intValue())
                .build();
    }

}