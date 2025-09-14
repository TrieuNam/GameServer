package com.SouthMillion.config_service.config.cache;

import org.SouthMillion.dto.config.ConfigEnvelope;

import java.util.Optional;

/** Cache facade: L1 (Caffeine) + optional L2 (Disk/Redis). */
public interface CacheTier {
    Optional<ConfigEnvelope> get(String path);
    void put(String path, ConfigEnvelope env);
    void evict(String path);
}