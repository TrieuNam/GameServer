package com.SouthMillion.config_service.config.cache;

import org.SouthMillion.dto.config.ConfigEnvelope;

import java.util.Optional;

/** L1 (Caffeine) + L2 (Disk). */
public interface CacheTier {
    Optional<ConfigEnvelope> get(String path);
    void put(String path, ConfigEnvelope env);
    void evict(String path);
}