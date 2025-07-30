package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDatumDTO {
    private long last_login_time;
    private String level;
    private String role_id;
    private String role_name;
    private String server_id;
    private String vip;
    // ...thêm trường khác nếu client cần
}