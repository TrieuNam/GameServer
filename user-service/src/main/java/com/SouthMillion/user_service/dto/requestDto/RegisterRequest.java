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
    private Integer accountType; // có thể mặc định 1
    private Integer fcmFlag;     // có thể mặc định 0
    private String openid;       // nếu cần, hoặc null
}