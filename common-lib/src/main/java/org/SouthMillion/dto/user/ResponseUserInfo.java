package org.SouthMillion.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class ResponseUserInfo {
    private List<ResponseServerData> role_data;
    private String account;
    private int account_type;
    private int fcm_flag;
    private String login_sign;
    private long login_time;
    private String uid;
    private String openid;
    private String merger_spid;
}