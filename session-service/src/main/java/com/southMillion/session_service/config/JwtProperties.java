package com.southMillion.session_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String issuer;
    private String secret;
    private long accessTtlSeconds;
    private long refreshTtlSeconds;
    private boolean rotateRefresh = true;
}