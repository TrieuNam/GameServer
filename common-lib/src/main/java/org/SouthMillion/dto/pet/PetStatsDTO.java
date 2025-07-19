package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PetStatsDTO {
    @JsonProperty("hp")
    private int hp;
    @JsonProperty("atk")
    private int atk;
    @JsonProperty("def")
    private int def;
    @JsonProperty("speed")
    private int speed;
}