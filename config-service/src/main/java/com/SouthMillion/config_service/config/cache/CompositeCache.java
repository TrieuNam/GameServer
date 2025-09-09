package com.SouthMillion.config_service.config.cache;

import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("compositeCacheL2")
public class CompositeCache implements CacheTier {

    private final L1CaffeineCache l1;
    private final L2DiskCache l2Opt; // có thể null nếu không khai báo

    public CompositeCache(L1CaffeineCache l1, org.springframework.beans.factory.ObjectProvider<L2DiskCache> l2Provider) {
        this.l1 = l1;
        this.l2Opt = l2Provider.getIfAvailable();
    }

    @Override
    public Optional<ConfigEnvelope> get(String path) {
        var v1 = l1.get(path);
        if (v1.isPresent()) return v1;
        if (l2Opt != null) {
            var v2 = l2Opt.get(path);
            v2.ifPresent(env -> l1.put(path, env)); // back-fill L1
            return v2;
        }
        return Optional.empty();
    }

    @Override
    public void put(String path, ConfigEnvelope env) {
        l1.put(path, env);
        if (l2Opt != null) l2Opt.put(path, env);
    }

    @Override
    public void evict(String path) {
        l1.evict(path);
        if (l2Opt != null) l2Opt.evict(path);
    }
}
