package com.SouthMillion.gateway_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
@Data
public class AppAuthProperties {
    private String tokenSecret;
    private long tokenExpiration;
}
