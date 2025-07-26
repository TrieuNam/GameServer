package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Data
@Setter
@Getter
public class RoleLevelChangeDTO implements Serializable {
    private Integer level;
    private Long exp;
    // getter & setter
}
