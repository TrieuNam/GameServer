package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MountJihuoDTO {
    @JsonProperty("mount_id")
    private int mountId;
    private int level;
    @JsonProperty("up_id")
    private int upId;
    @JsonProperty("up_num")
    private int upNum;
    @JsonProperty("explore_item_id")
    private int exploreItemId;
    @JsonProperty("explore_item_num")
    private int exploreItemNum;
    @JsonProperty("up_att")
    private List<JihuoAttDTO> upAtt;
}
