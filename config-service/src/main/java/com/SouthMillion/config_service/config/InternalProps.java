package com.SouthMillion.config_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal")
public record InternalProps(Boolean enabled, String token) {}