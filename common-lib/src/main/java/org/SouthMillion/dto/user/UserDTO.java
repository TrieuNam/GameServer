package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String account;
    private int account_type;
    private int fcm_flag;
    private String login_sign;
    private long login_time;
    private String uid;
    private String openid;
    private String merger_spid;
    // ...thêm trường khác nếu cần
}