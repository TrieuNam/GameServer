package com.SouthMillion.config_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.config")
public record ConfigProps(
        String  mode,               // classpath | filesystem
        String  classpathRoot,
        String  fsRoot,
        Integer cacheTtlSec,
        Integer cacheMaxSize,
        String  publicCacheControl,
        L2      l2
) {
    public record L2(
            Boolean enabled,
            String  dir,
            Long    maxBytesPerEntry
    ) {}
}