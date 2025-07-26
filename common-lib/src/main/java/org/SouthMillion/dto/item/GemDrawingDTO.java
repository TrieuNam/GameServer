package org.SouthMillion.dto.item;

import lombok.Data;
import org.SouthMillion.proto.Msgother.Msgother;

import java.util.List;

@Data
public class GemDrawingDTO {
    private Integer level;
    private List<GemInlayDTO> gemList;

    public Msgother.PB_GemDrawingData toProto() {
        Msgother.PB_GemDrawingData.Builder builder = Msgother.PB_GemDrawingData.newBuilder()
                .setLevel(level == null ? 0 : level);
        if (gemList != null) {
            for (GemInlayDTO inlay : gemList) {
                builder.addGemList(inlay.toProto());
            }
        }
        return builder.build();
    }
}