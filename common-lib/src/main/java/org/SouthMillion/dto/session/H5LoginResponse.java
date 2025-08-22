package org.SouthMillion.dto.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class H5LoginResponse {
    private int ret;
    private User user;
    private Map<String, RoleDatum> role_data;
    private String msg; // optional khi ret != 0

    public static H5LoginResponse error(int code, String msg){
        H5LoginResponse r = new H5LoginResponse();
        r.ret = code; r.msg = msg;
        return r;
    }

    @Data
    public static class User {
        private String account;
        private int account_type;
        private int fcm_flag;
        private String login_sign;
        private long login_time;
        private String uid;
        private String openid;
        private String merger_spid;
    }

    @Data
    public static class RoleDatum {
        private long last_login_time;
        private String level;
        private String role_id;
        private String role_name;
        private String server_id;
        private String vip;
    }
}