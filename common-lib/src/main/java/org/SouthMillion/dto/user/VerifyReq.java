package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VerifyReq {
    @NotBlank @Size(max = 64)
    private String username;

    @NotBlank @Size(max = 1000) // giới hạn hợp lý để tránh input quá lớn
    private String password;
}