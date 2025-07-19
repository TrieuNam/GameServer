package com.SouthMillion.user_service.dto.responseDto;

import lombok.Data;

@Data
public class RegisterResponse {
    private int ret;         // 0: OK, 1: trùng user, 2: lỗi khác
    private String message;
    private Long userId;
}