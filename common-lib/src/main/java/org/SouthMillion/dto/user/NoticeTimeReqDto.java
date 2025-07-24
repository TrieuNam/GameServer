package org.SouthMillion.dto.user;

import lombok.Data;

@Data
public class NoticeTimeReqDto {
    private Long userId;
    private Integer type; // 0: get, 1: set
    private Long param;   // Thời gian cần set nếu type=1
    // getter/setter
}

