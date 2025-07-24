package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class LimitCoreReqDto {
    private Long userId;
    private Integer type;
    private Integer p1;
    // getter/setter
}
