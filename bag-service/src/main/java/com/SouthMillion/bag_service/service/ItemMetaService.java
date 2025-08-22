package com.SouthMillion.bag_service.service;

import com.SouthMillion.bag_service.service.config.ItemMetaFeign;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemMetaService {

    private final ItemMetaFeign feign;
    private final LoadingCache<Integer, Meta> cache = Caffeine.newBuilder()
            .maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(10)).build(this::loadOne);

    public record Meta(int itemId, int pileLimit, boolean isVirtual, Integer normalizedId) {}

    private Meta loadOne(int id) {
        Map<String, Map<String,Object>> m = feign.batchMeta(String.valueOf(id));
        Map<String,Object> v = m.getOrDefault(String.valueOf(id), Map.of());
        int itemId = ((Number)v.getOrDefault("itemId", id)).intValue();
        int pile = ((Number)v.getOrDefault("pileLimit", 1)).intValue();
        boolean isVirtual = ((Number)v.getOrDefault("isVirtual", 0)).intValue() == 1;
        Integer norm = v.get("normalizedId")==null? null : ((Number)v.get("normalizedId")).intValue();
        return new Meta(itemId, Math.max(pile,1), isVirtual, norm);
    }

    public Map<Integer, Meta> getMetas(Collection<Integer> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(i->i, cache::get));
    }
}