package org.SouthMillion.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GoogleProfile {
    private String sub;      // Google user ID
    private String email;
    private String name;
    private String picture;  // avatar
    private String locale;
    private String aud;      // client_id (audience)
}
