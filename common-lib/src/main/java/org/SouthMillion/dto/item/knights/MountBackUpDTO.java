package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MountBackUpDTO {
    @JsonProperty("mount_id")
    private int mountId;
    private int level;
    @JsonProperty("up_item_id")
    private int upItemId;
    @JsonProperty("up_item_num")
    private int upItemNum;
}