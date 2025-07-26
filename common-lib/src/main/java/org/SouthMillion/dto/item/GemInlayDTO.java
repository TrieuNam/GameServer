package org.SouthMillion.dto.item;

import lombok.*;
import org.SouthMillion.proto.Msgother.Msgother;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GemInlayDTO {
    private Integer itemId;
    private Integer pos;
    public Msgother.PB_GemInlayData toProto() {
        return Msgother.PB_GemInlayData.newBuilder()
                .setItemId(itemId == null ? 0 : itemId)
                .setPos(pos == null ? 0 : pos)
                .build();
    }
}