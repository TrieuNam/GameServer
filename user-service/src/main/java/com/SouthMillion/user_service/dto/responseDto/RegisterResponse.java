package com.SouthMillion.user_service.dto.responseDto;

import lombok.Data;

@Data
public class RegisterResponse {
    private int ret;
    private String message;
    private Long userId;
    private String roleId;
    private String roleName;
}