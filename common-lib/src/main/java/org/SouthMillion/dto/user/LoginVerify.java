package org.SouthMillion.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class LoginVerify {
    private int ret; // 0 = success
    private ResponseUserInfo user; // thông tin user
    private List<ResponseServerData> role_data; // list role
}