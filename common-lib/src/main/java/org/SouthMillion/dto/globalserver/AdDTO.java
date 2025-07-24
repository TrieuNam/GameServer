package org.SouthMillion.dto.globalserver;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class AdDTO {
    private Integer todayCount;
    private Long nextFetchTime;
    private Boolean canFetch; // Dùng cho logic client (còn lượt xem/ngày không?)
}