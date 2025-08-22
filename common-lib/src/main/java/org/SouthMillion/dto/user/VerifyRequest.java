package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyRequest {
    @NotBlank
    private String username;
    @NotBlank private String password;
}