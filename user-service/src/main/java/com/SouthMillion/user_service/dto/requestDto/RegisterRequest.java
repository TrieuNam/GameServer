package com.SouthMillion.user_service.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private Integer accountType;
    private Integer fcmFlag;
    private String openid;

    // Thêm các field cho role mặc định (nếu muốn nhận từ client)
    private String roleName;
    private String serverId;
    private int level;
    private String vip;
}