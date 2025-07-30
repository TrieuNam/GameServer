package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MountLevelDTO {
    @JsonProperty("mount_id")
    private int mountId;
    private int level;
    @JsonProperty("show_mount_level")
    private int showMountLevel;
    @JsonProperty("mount_level")
    private int mountLevel;
    @JsonProperty("up_att")
    private List<JihuoAttDTO> upAtt;
    @JsonProperty("up_item_id")
    private int upItemId;
    @JsonProperty("up_item_num")
    private int upItemNum;
}