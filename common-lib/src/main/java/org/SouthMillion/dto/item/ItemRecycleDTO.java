package org.SouthMillion.dto.item;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.proto.Msgother.Msgother;

@Data
@Setter
@Getter
public class ItemRecycleDTO {
    private Integer level;
    private Long exp;
    // getters/setters

    public Msgother.PB_SCItemRecycleInfo toProto() {
        return Msgother.PB_SCItemRecycleInfo.newBuilder()
                .setLevel(level == null ? 0 : level)
                .setExp(exp == null ? 0 : exp)
                .build();
    }
}
