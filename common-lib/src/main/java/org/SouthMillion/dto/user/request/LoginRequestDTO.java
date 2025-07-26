package org.SouthMillion.dto.user.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class LoginRequestDTO implements Serializable {
    private String username;
    private String password;
    private String roleName;
}