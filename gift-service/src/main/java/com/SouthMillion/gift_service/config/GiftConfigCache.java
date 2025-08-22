package com.SouthMillion.gift_service.config;


import com.SouthMillion.gift_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GiftConfigCache {

    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.gift.cache-seconds:30}")
    private int cacheSeconds;

    private final Cache<String, JsonNode> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(16)
            .build();

    private final Map<String, String> etags = new ConcurrentHashMap<>();

    public JsonNode giftJson() {
        return getJson("gift.json");
    }

    private JsonNode getJson(String path) {
        JsonNode cached = cache.getIfPresent(path);
        String etag = etags.get(path);
        try {
            ResponseEntity<byte[]> resp = configFeign.getItem(path, etag);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                JsonNode node = om.readTree(json);
                cache.put(path, node);
                Optional.ofNullable(resp.getHeaders().getETag()).ifPresent(t -> etags.put(path, t));
                return node;
            }
            if (resp.getStatusCode().value() == 304 && cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("gift-config fetch failed, fallback cache: {}", e.toString());
        }
        return cached;
    }

    // ====== Structures for runtime ======

    @Getter
    public static class GiftItem {
        private final long itemId;
        private final long count;
        private final int rate; // weight

        public GiftItem(long itemId, long count, int rate) {
            this.itemId = itemId; this.count = count; this.rate = rate;
        }
    }

    @Getter
    public static class GiftBox {
        private final long id;
        private final int giftType;  // 1=DefGift, 2=RandGift
        private final int randNum;   // số lần rút khi mở một hộp
        private final List<GiftItem> items;

        public GiftBox(long id, int giftType, int randNum, List<GiftItem> items) {
            this.id = id; this.giftType = giftType; this.randNum = randNum; this.items = items;
        }
    }

    /** Parse toàn bộ gift.json thành map: giftId -> GiftBox */
    public Map<Long, GiftBox> compile() {
        JsonNode root = giftJson();
        if (root == null) return Map.of();
        Map<Long, GiftBox> map = new HashMap<>();

        // DefGift
        JsonNode def = root.path("DefGift");
        if (def.isArray()) {
            for (JsonNode n : def) {
                long id = n.path("id").asLong();
                int randNum = n.path("rand_num").asInt(0);
                List<GiftItem> items = new ArrayList<>();
                for (JsonNode g : n.withArray("gift")) {
                    items.add(new GiftItem(g.path("item_id").asLong(), g.path("num").asLong(), g.path("rate").asInt(1)));
                }
                map.put(id, new GiftBox(id, 1, Math.max(1, randNum), items));
            }
        }

        // RandGift
        JsonNode rng = root.path("RandGift");
        if (rng.isArray()) {
            for (JsonNode n : rng) {
                long id = n.path("id").asLong();
                int randNum = n.path("rand_num").asInt(1);
                List<GiftItem> items = new ArrayList<>();
                for (JsonNode g : n.withArray("gift")) {
                    items.add(new GiftItem(g.path("item_id").asLong(), g.path("num").asLong(), g.path("rate").asInt(1)));
                }
                map.put(id, new GiftBox(id, 2, Math.max(1, randNum), items));
            }
        }
        return map;
    }
}