package org.SouthMillion.dto.user.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RegisterResponseDTO {
    private boolean success;
    private String message;
    private Integer uid;
    // getter/setter
}