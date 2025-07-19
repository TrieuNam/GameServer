package org.SouthMillion.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AdRewardDTO {
    @JsonProperty("userId")
    private Long userId;
    @JsonProperty("adToken")
    private String adToken;
    @JsonProperty("adProvider")
    private String adProvider; // "admob", "unityads", ...
}
