package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    private String platUserName;
    private Long createTime;
    private Long lastLoginTime;
    private Long forbidTime;
}