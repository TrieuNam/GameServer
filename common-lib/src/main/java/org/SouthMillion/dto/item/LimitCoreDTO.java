package org.SouthMillion.dto.item;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class LimitCoreDTO {
    private List<Integer> coreLevel;
    // getters/setters
}