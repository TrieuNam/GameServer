package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MountResDTO {
    @JsonProperty("mount_skin_seq")
    private int mountSkinSeq;
    @JsonProperty("jihuo_item_id")
    private int jihuoItemId;
    @JsonProperty("jihuo_att")
    private List<JihuoAttDTO> jihuoAtt;
    @JsonProperty("jihuo")
    private List<RewardItemDTO> jihuo;
}