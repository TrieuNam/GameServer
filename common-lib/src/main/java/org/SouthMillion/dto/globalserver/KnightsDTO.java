package org.SouthMillion.dto.globalserver;

import lombok.*;
import org.SouthMillion.proto.Msgother.Msgother;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Setter
@Getter
public class KnightsDTO {
    private Integer level;
    private Integer flag;
    private Integer levelFlag;

    public Msgother.PB_SCKnightsInfo toProto() {
        return Msgother.PB_SCKnightsInfo.newBuilder()
                .setLevel(level == null ? 0 : level)
                .setFlag(flag == null ? 0 : flag)
                .setLevelFlag(levelFlag == null ? 0 : levelFlag)
                .build();
    }
}