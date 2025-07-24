package org.SouthMillion.dto.globalserver;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class DuobaoDTO {
    private Integer integral;
    private Integer fetchFlag;
    private Integer freeRefreshNum;
    private Long freeRefreshTime;
    // getters/setters
}
