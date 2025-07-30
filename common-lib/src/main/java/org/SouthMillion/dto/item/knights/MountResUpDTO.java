package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MountResUpDTO {
    @JsonProperty("mount_skin_seq")
    private int mountSkinSeq;
    @JsonProperty("skin_level")
    private int skinLevel;
    @JsonProperty("up_item_id")
    private int upItemId;
    @JsonProperty("up_item_num")
    private int upItemNum;
    @JsonProperty("jihuo_att")
    private List<JihuoAttDTO> jihuoAtt;
}