package org.SouthMillion.dto.user.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class LoginResponseDTO implements Serializable {
    private int result;         // 0: ok, 1: sai password, 2: user không tồn tại
    private String message;
    private int forbidTime;
}