package org.SouthMillion.dto.globalserver;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.proto.Msgother.Msgother;

@Data
@Setter
@Getter
public class DuobaoDTO {
    private Integer integral;
    private Integer fetchFlag;
    private Integer freeRefreshNum;
    private Long freeRefreshTime;


    public Msgother.PB_DuoBaoData toProto() {
        return Msgother.PB_DuoBaoData.newBuilder()
                .setIntegral(integral == null ? 0 : integral)
                .setFetchFlag(fetchFlag == null ? 0 : fetchFlag)
                .setFreeRefreshNum(freeRefreshNum == null ? 0 : freeRefreshNum)
                .setFreeRefreshTime(freeRefreshTime == null ? 0 : freeRefreshTime.intValue())
                .build();
    }
}
