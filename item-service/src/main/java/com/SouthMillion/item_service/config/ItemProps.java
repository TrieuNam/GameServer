package com.SouthMillion.item_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "item.config")
public record ItemProps(
        String baseUrl,
        List<String> itemCatalogPaths,
        int cacheTtlSeconds,
        Integer cacheMaxSize,
        Integer metaRedisTtlSeconds,
        String metaRedisKeyPrefix
) {}