package com.SouthMillion.item_service.config;

import com.SouthMillion.item_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConfigClient {

    private final ConfigFeign feign;
    private final ObjectMapper om = new ObjectMapper();

    @Getter
    public static class CacheEntry {
        public final String key;             // ví dụ: config/gameworld/item/other
        public volatile String etag;         // từ header ETag
        public volatile String revision;     // từ header X-Config-Revision
        public volatile byte[] body;         // bytes content
        public CacheEntry(String key, String etag, String revision, byte[] body) {
            this.key = key; this.etag = etag; this.revision = revision; this.body = body;
        }
    }

    // key = "config/gameworld/item/{name}" hoặc "config/gameworld/logic/{leaf}"
    private final Map<String, CacheEntry> l1 = new ConcurrentHashMap<>();

    public List<String> listItems() {
        return feign.list("item", 0, 200);
    }

    public Payload getItemLeaf(String name) {
        String key = "config/gameworld/item/" + name;
        String ifNone = Optional.ofNullable(l1.get(key)).map(e -> e.etag).orElse(null);

        ResponseEntity<byte[]> resp = feign.getItem(name, ifNone);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String etag = resp.getHeaders().getETag();
            String rev  = resp.getHeaders().getFirst("X-Config-Revision");
            byte[] body = resp.getBody();
            l1.put(key, new CacheEntry(key, etag, rev, body));
            return new Payload(body, rev, etag, true);
        }
        // 304 Not Modified -> dùng cache
        CacheEntry ce = l1.get(key);
        if (ce == null) throw new IllegalStateException("No cache for key="+key+" (304 without cache)");
        return new Payload(ce.body, ce.revision, ce.etag, false);
    }

    public Payload getLogicLeaf(String leafUnderLogic) {
        // leafUnderLogic ví dụ: "logicconfig/bag_cfg.json" hoặc "logicconfig/gem_item.json"
        String key = "config/gameworld/logic/" + leafUnderLogic;
        String ifNone = Optional.ofNullable(l1.get(key)).map(e -> e.etag).orElse(null);

        ResponseEntity<byte[]> resp = feign.getLogic(leafUnderLogic, ifNone);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String etag = resp.getHeaders().getETag();
            String rev  = resp.getHeaders().getFirst("X-Config-Revision");
            byte[] body = resp.getBody();
            l1.put(key, new CacheEntry(key, etag, rev, body));
            return new Payload(body, rev, etag, true);
        }
        CacheEntry ce = l1.get(key);
        if (ce == null) throw new IllegalStateException("No cache for key="+key+" (304 without cache)");
        return new Payload(ce.body, ce.revision, ce.etag, false);
    }

    // Lấy nhiều item qua /bundle để warmup nhanh
    public Map<String, Payload> getItemsByBundle(Collection<String> itemLeaves) {
        // keysCsv = "config/gameworld/item/a,config/gameworld/item/b"
        String keysCsv = itemLeaves.stream()
                .map(leaf -> "config/gameworld/item/" + leaf)
                .collect(Collectors.joining(","));
        ResponseEntity<List<ConfigEnvelope<String>>> resp = feign.bundle(keysCsv);
        var out = new HashMap<String, Payload>();
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return out;

        for (var env : resp.getBody()) {
            byte[] body = env.content() == null ? new byte[0] : env.content().getBytes(StandardCharsets.UTF_8);
            l1.put(env.key(), new CacheEntry(env.key(), env.etag(), env.revision(), body));
            out.put(env.key(), new Payload(body, env.revision(), env.etag(), true));
        }
        return out;
    }

    public record Payload(byte[] body, String revision, String etag, boolean fromNetwork) {}
}