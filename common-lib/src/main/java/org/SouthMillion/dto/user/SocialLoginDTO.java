package org.SouthMillion.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SocialLoginDTO {
    @JsonProperty("provider")  // "google", "facebook", "apple"
    private String provider;
    @JsonProperty("token")     // id_token (Google/Apple) hoặc access_token (Facebook)
    private String token;
}
