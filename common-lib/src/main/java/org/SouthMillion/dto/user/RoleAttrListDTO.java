package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
@Setter
@Getter
public class RoleAttrListDTO implements Serializable {
    private Integer notifyReason;
    private Long capability;
    private List<AttrPairDTO> attrList;
    // getter & setter
}
