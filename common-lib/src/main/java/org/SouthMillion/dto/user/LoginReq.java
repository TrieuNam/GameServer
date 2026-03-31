package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginReq {
    @NotBlank(message = "Account or username is required")
    private String accountOrUsername;
    
    @NotBlank(message = "Password is required")
    private String password;
}
