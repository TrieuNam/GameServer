package org.SouthMillion.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BlockUserDTO {
    @JsonProperty("userId")
    private Long userId;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("status")
    private String status; // "banned", "muted", etc.
}
