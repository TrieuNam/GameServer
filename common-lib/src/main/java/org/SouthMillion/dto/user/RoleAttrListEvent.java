package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleAttrListEvent {
    private RoleDto role;
    private List<RoleAttrDto> attrs;
}
