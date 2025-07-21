package org.SouthMillion.dto.user;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseServerData {
    private long last_login_time;
    private int level;
    private String role_id;
    private String role_name;
    private String server_id;
    private String vip;
}
