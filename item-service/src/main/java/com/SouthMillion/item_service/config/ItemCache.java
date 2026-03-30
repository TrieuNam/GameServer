package com.SouthMillion.item_service.config;

import com.SouthMillion.item_service.service.client.ConfigServiceFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ItemCache {

    public static final class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(int itemId) { super("Item not found: " + itemId); }
    }

    private record Entry(ItemMetaDTO meta, String etag, long cachedAtSec) {}
    private record CatalogEntry(JsonNode body, String etag) {}

    private final ConfigServiceFeign feign;
    private final ItemProps props;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Counter redisHitCounter;
    private final Counter redisMissCounter;

    // Cache chính: itemId -> Entry
    private final ConcurrentHashMap<Integer, Entry> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CatalogEntry> catalogCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> itemCatalogIndex = new ConcurrentHashMap<>();

    // (tuỳ chọn) thêm Caffeine chỉ để TTL mềm
    private final Cache<Integer, Boolean> ttl = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(300))
            .maximumSize(10_000)
            .build();

    public ItemCache(ConfigServiceFeign feign,
                     ItemProps props,
                     StringRedisTemplate redis,
                     ObjectMapper objectMapper,
                     MeterRegistry meterRegistry) {
        this.feign = feign;
        this.props = props;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.redisHitCounter = meterRegistry.counter("item.meta.redis.hit");
        this.redisMissCounter = meterRegistry.counter("item.meta.redis.miss");
    }

    public ItemMetaDTO getOrLoad(int itemId) {
        var e = map.get(itemId);
        if (e != null) {
            ttl.put(itemId, Boolean.TRUE);
            return e.meta;
        }

        ItemMetaDTO fromRedis = getMetaFromRedis(itemId);
        if (fromRedis != null) {
            map.put(itemId, new Entry(fromRedis, null, nowSec()));
            ttl.put(itemId, Boolean.TRUE);
            return fromRedis;
        }

        JsonNode itemNode = findItemNode(itemId);
        if (itemNode == null) {
            map.remove(itemId);
            throw new ItemNotFoundException(itemId);
        }

        ItemMetaDTO dto = toDTO(itemId, itemNode);
        Entry ne = new Entry(dto, null, nowSec());
        map.put(itemId, ne);
        ttl.put(itemId, Boolean.TRUE);
        putMetaToRedis(itemId, dto);
        return dto;
    }

    public JsonNode getRaw(int itemId) {
        JsonNode itemNode = findItemNode(itemId);
        if (itemNode != null) return itemNode;
        throw new ItemNotFoundException(itemId);
    }

    public boolean evictMeta(int itemId) {
        map.remove(itemId);
        ttl.invalidate(itemId);
        itemCatalogIndex.remove(itemId);
        String key = redisMetaKey(itemId);
        try {
            return Boolean.TRUE.equals(redis.delete(key));
        } catch (Exception e) {
            log.debug("[ItemCache] redis meta evict failed itemId={} key={} ex={}", itemId, key, e.toString());
            return false;
        }
    }

    public int evictAllMeta() {
        int localCount = map.size();
        map.clear();
        ttl.invalidateAll();
        itemCatalogIndex.clear();

        int redisCount = 0;
        String pattern = redisMetaKey("*");
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redis.delete(keys);
                redisCount = deleted == null ? 0 : deleted.intValue();
            }
        } catch (Exception e) {
            log.debug("[ItemCache] redis meta evict-all failed pattern={} ex={}", pattern, e.toString());
        }

        return Math.max(localCount, redisCount);
    }

    private static long nowSec() { return System.currentTimeMillis() / 1000; }

    private ItemMetaDTO getMetaFromRedis(int itemId) {
        String key = redisMetaKey(itemId);
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                redisMissCounter.increment();
                return null;
            }
            ItemMetaDTO dto = objectMapper.readValue(raw, ItemMetaDTO.class);
            redisHitCounter.increment();
            return dto;
        } catch (Exception e) {
            redisMissCounter.increment();
            log.debug("[ItemCache] redis meta read failed itemId={} key={} ex={}", itemId, key, e.toString());
            return null;
        }
    }

    private void putMetaToRedis(int itemId, ItemMetaDTO dto) {
        if (dto == null) {
            return;
        }
        String key = redisMetaKey(itemId);
        try {
            int ttlSeconds = redisMetaTtlSeconds();
            String encoded = objectMapper.writeValueAsString(dto);
            if (ttlSeconds > 0) {
                redis.opsForValue().set(key, encoded, ttlSeconds, TimeUnit.SECONDS);
            } else {
                redis.opsForValue().set(key, encoded);
            }
        } catch (Exception e) {
            log.debug("[ItemCache] redis meta write failed itemId={} key={} ex={}", itemId, key, e.toString());
        }
    }

    private int redisMetaTtlSeconds() {
        Integer configured = props.metaRedisTtlSeconds();
        if (configured != null) {
            return Math.max(0, configured);
        }
        return Math.max(0, props.cacheTtlSeconds());
    }

    private String redisMetaKey(int itemId) {
        return redisMetaKey(String.valueOf(itemId));
    }

    private String redisMetaKey(String suffix) {
        String prefix = props.metaRedisKeyPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "item:meta:";
        }
        return prefix + suffix;
    }

    private JsonNode findItemNode(int itemId) {
        for (String catalogPath : candidateCatalogPaths(itemId)) {
            JsonNode catalog = loadCatalog(catalogPath);
            JsonNode found = findItemNodeRecursive(catalog, itemId);
            if (found != null) {
                itemCatalogIndex.put(itemId, catalogPath);
                return found;
            }
        }
        itemCatalogIndex.remove(itemId);
        return null;
    }

    private List<String> candidateCatalogPaths(int itemId) {
        Set<String> ordered = new LinkedHashSet<>();
        String indexedPath = itemCatalogIndex.get(itemId);
        if (indexedPath != null && !indexedPath.isBlank()) ordered.add(indexedPath);
        List<String> configured = props.itemCatalogPaths();
        if (configured != null) ordered.addAll(configured);
        return new ArrayList<>(ordered);
    }

    private JsonNode loadCatalog(String path) {
        CatalogEntry cached = catalogCache.get(path);
        String etag = cached == null ? null : cached.etag;

        try {
            ResponseEntity<JsonNode> resp = feign.getFile(path, etag);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                CatalogEntry updated = new CatalogEntry(resp.getBody(), resp.getHeaders().getETag());
                catalogCache.put(path, updated);
                return updated.body;
            }
        } catch (FeignConfig.NotModifiedException nme) {
            if (cached != null) return cached.body;
        } catch (feign.FeignException.NotFound nf) {
            catalogCache.remove(path);
            return null;
        } catch (feign.FeignException fe) {
            // config-service returned non-404 error or is unreachable — fall back to cache
            log.warn("[ItemCache] config-service error loading catalog '{}' status={} — {}",
                    path, fe.status(), fe.getMessage());
            if (cached != null) {
                return cached.body;
            }
            return null;
        } catch (Exception ex) {
            // unexpected error (e.g. JSON decode failure) — fall back to cache
            log.warn("[ItemCache] unexpected error loading catalog '{}' — {}", path, ex.getMessage());
            if (cached != null) {
                return cached.body;
            }
            return null;
        }

        return cached == null ? null : cached.body;
    }

    private JsonNode findItemNodeRecursive(JsonNode node, int itemId) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;

        if (node.isObject()) {
            JsonNode idNode = node.get("id");
            if (matchesItemId(idNode, itemId) && looksLikeItemNode(node)) {
                return node;
            }
            for (JsonNode child : node) {
                JsonNode found = findItemNodeRecursive(child, itemId);
                if (found != null) return found;
            }
            return null;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findItemNodeRecursive(child, itemId);
                if (found != null) return found;
            }
        }

        return null;
    }

    private boolean matchesItemId(JsonNode idNode, int itemId) {
        if (idNode == null || idNode.isNull()) return false;
        if (idNode.canConvertToInt()) return idNode.asInt() == itemId;
        return String.valueOf(itemId).equals(idNode.asText());
    }

    private boolean looksLikeItemNode(JsonNode node) {
        return node.has("item_type") || node.has("sellprice") || node.has("pile_limit") || node.has("name");
    }

    private static ItemMetaDTO toDTO(int itemId, JsonNode j) {
        // Map tuỳ theo schema file item của bạn:
        String itemType = switch (j.path("item_type").asInt(0)) {
            case 2 -> "equip";
            case 1 -> "consumable";
            case 0 -> "currency";
            default -> "unknown";
        };
        Integer quality    = j.has("quality")      ? j.get("quality").asInt()         : (j.has("color") ? j.get("color").asInt() : null);
        Long    exp        = j.has("exp")          ? j.get("exp").asLong()            : null;
        Long    sellPrice  = j.has("sellprice")    ? j.get("sellprice").asLong()      : null;
        Integer pileLimit  = j.has("pile_limit")   ? j.get("pile_limit").asInt()      : null;
        Long    invalid    = j.has("invalid_time") ? j.get("invalid_time").asLong()   : null;
        Boolean isSpecial  = j.has("is_special")   ? j.get("is_special").asInt() == 1 : null;
        Boolean isVirtual  = j.has("is_virtual")   ? j.get("is_virtual").asInt() == 1 : null;

        return new ItemMetaDTO(itemId, itemType, quality, exp, sellPrice, pileLimit, invalid, isSpecial, isVirtual);
    }
}