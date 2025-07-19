package com.SouthMillion.battleserver_service.dto.knights.knight;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnightsBookDto {
    private String level;
    private String seq;
    private String condition;
    @JsonProperty("param_1")
    private String param1;
    @JsonProperty("att_type")
    private String attType;
    @JsonProperty("att_num")
    private String attNum;
}
