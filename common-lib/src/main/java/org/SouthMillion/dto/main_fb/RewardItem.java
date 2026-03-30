package org.SouthMillion.dto.main_fb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardItem {
    private Long itemId;
    private Integer count;
}
