package com.SouthMillion.config_service.config.cache;

import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("compositeCacheL1")
@Primary
public class CompositeCacheL1L2 implements CacheTier {

    private final L1CaffeineCache l1;
    private final L2DiskCache l2;

    public CompositeCacheL1L2(L1CaffeineCache l1, L2DiskCache l2) {
        this.l1 = l1;
        this.l2 = l2; // l2 tự no-op nếu disabled
    }

    @Override
    public Optional<ConfigEnvelope> get(String path) {
        var v1 = l1.get(path);
        if (v1.isPresent()) return v1;

        var v2 = l2.get(path);
        v2.ifPresent(env -> l1.put(path, env)); // warm L1
        return v2;
    }

    @Override
    public void put(String path, ConfigEnvelope env) {
        l1.put(path, env);
        l2.put(path, env); // write-through
    }

    @Override
    public void evict(String path) {
        l1.evict(path);
        l2.evict(path);
    }
}