package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleExpChangeEvent {
    private String roleId;
    private Long curExp;
    private int changeExp;
}