package com.SouthMillion.item_service.config;

import com.SouthMillion.item_service.service.client.ConfigServiceFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ItemCache {

    public static final class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(int itemId) { super("Item not found: " + itemId); }
    }

    private record Entry(ItemMetaDTO meta, String etag, long cachedAtSec) {}

    private final ConfigServiceFeign feign;
    private final ItemProps props;

    // Cache chính: itemId -> Entry
    private final ConcurrentHashMap<Integer, Entry> map = new ConcurrentHashMap<>();

    // (tuỳ chọn) thêm Caffeine chỉ để TTL mềm
    private final Cache<Integer, Boolean> ttl = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(300))
            .maximumSize(10_000)
            .build();

    public ItemCache(ConfigServiceFeign feign, ItemProps props) {
        this.feign = feign;
        this.props = props;
    }

    public ItemMetaDTO getOrLoad(int itemId) {
        var e = map.get(itemId);
        String etag = e == null ? null : e.etag;

        try {
            String path = props.itemPathTemplate().formatted(itemId);
            ResponseEntity<JsonNode> resp = feign.getFile(path, etag);
            int code = resp.getStatusCode().value();

            if (code == 200 && resp.getBody() != null) {
                String newEtag = resp.getHeaders().getETag();
                ItemMetaDTO dto = toDTO(itemId, resp.getBody());
                Entry ne = new Entry(dto, newEtag, nowSec());
                map.put(itemId, ne);
                ttl.put(itemId, Boolean.TRUE);
                return dto;
            }

            // 204 (nếu có) hoặc bất kỳ tình huống khác mà không có body → xem như missing
            map.remove(itemId);
            throw new ItemNotFoundException(itemId);

        } catch (FeignConfig.NotModifiedException nme) {
            // 304 → dùng lại cache
            if (e != null) {
                ttl.put(itemId, Boolean.TRUE);
                return e.meta;
            }
            // 304 nhưng chưa có cache → xem như missing
            throw new ItemNotFoundException(itemId);
        } catch (feign.FeignException.NotFound nf) {
            map.remove(itemId);
            throw new ItemNotFoundException(itemId);
        }
    }

    public JsonNode getRaw(int itemId) {
        String path = props.itemPathTemplate().formatted(itemId);
        try {
            var resp = feign.getFile(path, null);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody();
            }
        } catch (feign.FeignException.NotFound nf) {
            // fallthrough
        }
        throw new ItemNotFoundException(itemId);
    }

    private static long nowSec() { return System.currentTimeMillis() / 1000; }

    private static ItemMetaDTO toDTO(int itemId, JsonNode j) {
        // Map tuỳ theo schema file item của bạn:
        String itemType = switch (j.path("item_type").asInt(0)) {
            case 2 -> "equip";
            case 1 -> "consumable";
            case 0 -> "currency";
            default -> "unknown";
        };
        Integer quality    = j.has("quality")      ? j.get("quality").asInt()         : null;
        Long    exp        = j.has("exp")          ? j.get("exp").asLong()            : null;
        Long    sellPrice  = j.has("sellprice")    ? j.get("sellprice").asLong()      : null;
        Integer pileLimit  = j.has("pile_limit")   ? j.get("pile_limit").asInt()      : null;
        Long    invalid    = j.has("invalid_time") ? j.get("invalid_time").asLong()   : null;
        Boolean isSpecial  = j.has("is_special")   ? j.get("is_special").asInt() == 1 : null;

        return new ItemMetaDTO(itemId, itemType, quality, exp, sellPrice, pileLimit, invalid, isSpecial);
    }
}