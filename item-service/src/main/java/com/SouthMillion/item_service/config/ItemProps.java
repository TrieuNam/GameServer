package com.SouthMillion.item_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "item.config")
public record ItemProps(
        String baseUrl,
        String itemPathTemplate,
        int cacheTtlSeconds,
        Integer cacheMaxSize
) {}