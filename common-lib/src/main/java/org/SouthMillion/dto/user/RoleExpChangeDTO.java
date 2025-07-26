package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class RoleExpChangeDTO implements Serializable {
    private Long changeExp;
    private Long curExp;
    // getter & setter
}