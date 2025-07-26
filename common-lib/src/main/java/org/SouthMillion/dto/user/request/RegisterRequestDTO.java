package org.SouthMillion.dto.user.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class RegisterRequestDTO {
    private String pname;        // Tài khoản
    private String password;     // Mật khẩu (nếu có)
    private String deviceId;
    private String roleName;     // Tên nhân vật
    // Thêm field nếu muốn (server, platSpid...)

    // getter/setter
}