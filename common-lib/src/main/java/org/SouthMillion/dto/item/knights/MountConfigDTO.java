package org.SouthMillion.dto.item.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MountConfigDTO {
    @JsonProperty("mount_cfg")
    private List<MountLevelDTO> mountCfg;
    @JsonProperty("mount_jihuo")
    private List<MountJihuoDTO> mountJihuo;
    @JsonProperty("back_up")
    private List<MountBackUpDTO> backUp;
    @JsonProperty("mount_res")
    private List<MountResDTO> mountRes;
    @JsonProperty("mount_res_up")
    private List<MountResUpDTO> mountResUp;
    @JsonProperty("harness_gem_use")
    private List<HarnessGemUseDTO> harnessGemUse;
    @JsonProperty("harness_add")
    private List<HarnessAddDTO> harnessAdd;
    @JsonProperty("harness_buy")
    private List<HarnessBuyDTO> harnessBuy;
    // các field khác nếu cần
}