package org.SouthMillion.dto.user.response;

import lombok.*;
import org.SouthMillion.dto.user.RoleDatumDTO;
import org.SouthMillion.dto.user.UserDTO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    private int ret; // kết quả login: 0 = ok, 1 = sai, 2 = banned
    private String msg;
    private UserDTO user;
    private Map<String, RoleDatumDTO> role_data;
}