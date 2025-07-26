package org.SouthMillion.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RoleSystemSetDTO implements Serializable {
    private List<SystemSetDTO> systemSetList;
    // getter & setter
}
