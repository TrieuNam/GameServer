package org.SouthMillion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ItemDTO {

    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("num")
    private Integer num;

    // Convert sang proto
    public Msgknapsack.PB_ItemData toProto() {
        return Msgknapsack.PB_ItemData.newBuilder()
                .setItemId(itemId == null ? 0 : itemId)
                .setNum(num == null ? 0 : num)
                .build();
    }
}