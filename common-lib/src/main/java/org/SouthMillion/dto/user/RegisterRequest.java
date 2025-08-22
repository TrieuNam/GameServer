package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank private String password;
    private String spid;     // optional (điền khi tạo tài khoản gắn kênh)
    private String device;   // optional
}
