package org.SouthMillion.dto.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleReq {
    @NotBlank
    private String userId;
    @NotBlank
    private String name;   // <-- dùng name
    private Integer job;
    private Integer gender;
    private String serverId;
}