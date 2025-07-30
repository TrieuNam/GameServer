package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LimitCoreDTO {
    @JsonProperty("limit_tpye")
    private Integer limitTpye;       // kiểu giới hạn (typo đúng có thể là "limitType")

    @JsonProperty("limit_level")
    private Integer limitLevel;      // cấp độ giới hạn

    private Integer parm;            // tham số (ví dụ số lần)

    @JsonProperty("need_item_id")
    private Integer needItemId;      // item cần để mở khóa

    @JsonProperty("need_core_num")
    private Integer needCoreNum;     // số lượng item cần
}
