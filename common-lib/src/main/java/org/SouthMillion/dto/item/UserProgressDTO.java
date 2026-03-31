package org.SouthMillion.dto.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing user progress update for manual/handbook level-up
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressDTO {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("added_exp")
    private long addedExp;

    @JsonProperty("source")
    private String source;
}
