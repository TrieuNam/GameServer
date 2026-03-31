package com.SouthMillion.config_service.config.cache;

import com.SouthMillion.config_service.config.ConfigProps;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class L1CaffeineCache implements CacheTier {

    private final Cache<String, ConfigEnvelope> cache;

    public L1CaffeineCache(ConfigProps props) {
        int ttlSec  = props.cacheTtlSec()  != null ? props.cacheTtlSec()  : 300;
        int maxSize = props.cacheMaxSize() != null ? props.cacheMaxSize() : 2000;

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSec, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Optional<ConfigEnvelope> get(String path) {
        if (path == null || path.isBlank()) return Optional.empty();
        return Optional.ofNullable(cache.getIfPresent(path));
    }

    @Override
    public void put(String path, ConfigEnvelope env) {
        if (path == null || path.isBlank() || env == null) return;
        cache.put(path, env);
    }

    @Override
    public void evict(String path) {
        if (path == null || path.isBlank()) return;
        cache.invalidate(path);
    }
}