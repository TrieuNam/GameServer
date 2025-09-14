package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResp {
    private String userId;
    private String account;
    private String username;
    private String status;
}