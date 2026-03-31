package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {
    @NotBlank
    @Size(max = 64)
    private String account;

    @NotBlank @Size(max = 64)
    private String username;

    @NotBlank @Size(min = 6, max = 100)
    private String password;
}