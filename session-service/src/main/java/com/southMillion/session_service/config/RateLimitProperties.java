package com.southMillion.session_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int windowSeconds = 60;
    private int loginLimit = 10;
    private int refreshLimit = 60;
    private int genericLimit = 300;
}