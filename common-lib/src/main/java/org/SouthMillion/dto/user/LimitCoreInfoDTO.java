package org.SouthMillion.dto.user;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class LimitCoreInfoDTO implements Serializable {
    private List<Integer> coreLevel;
    // getter & setter
}
