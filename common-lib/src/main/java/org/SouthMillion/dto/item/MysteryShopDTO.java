package org.SouthMillion.dto.item;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.proto.Msgother.Msgother;

import java.util.List;

@Data
public class MysteryShopDTO {
    private Integer buyFlag;
    private List<Integer> indexList;


    public Msgother.PB_SCMysteryShopInfo toProto() {
        Msgother.PB_SCMysteryShopInfo.Builder builder = Msgother.PB_SCMysteryShopInfo.newBuilder()
                .setBuyFlag(buyFlag == null ? 0 : buyFlag);
        if (indexList != null) builder.addAllIndexList(indexList);
        return builder.build();
    }
}