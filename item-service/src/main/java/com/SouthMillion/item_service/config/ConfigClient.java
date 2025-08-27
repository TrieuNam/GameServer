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
        String raw = (name == null ? "" : name).replaceAll("^/+", "").replaceAll("/+$", "");
        boolean isExpenseShort = "expense/expense".equals(raw);

        // Gọi server bằng leaf đúng (expense -> expense/expense),
        // nhưng cache key chính vẫn là ".../expense" theo ý bạn
        String feignLeaf = isExpenseShort ? "expense" : raw;

        String keyPrimary = "config/gameworld/item/" + (isExpenseShort ? "expense" : raw);
        String keyAlias   = isExpenseShort ? "config/gameworld/item/expense" : keyPrimary;

        // Ưu tiên lấy ETag từ keyPrimary; nếu chưa có, thử alias
        String ifNone = Optional.ofNullable(l1.get(keyPrimary)).map(e -> e.etag)
                .orElseGet(() -> Optional.ofNullable(l1.get(keyAlias)).map(e -> e.etag).orElse(null));

        ResponseEntity<byte[]> resp = feign.getItem(feignLeaf, ifNone);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String etag = resp.getHeaders().getETag();
            String rev  = resp.getHeaders().getFirst("X-Config-Revision");
            byte[] body = resp.getBody();

            // Ghi cả 2 key để tránh lệch cache (an toàn khi bạn từng cache bằng alias)
            l1.put(keyPrimary, new CacheEntry(keyPrimary, etag, rev, body));
            if (!keyAlias.equals(keyPrimary)) {
                l1.put(keyAlias, new CacheEntry(keyAlias, etag, rev, body));
            }
            return new Payload(body, rev, etag, true);
        }

        // 304 hoặc lỗi mạng -> dùng cache (primary trước, alias sau)
        CacheEntry ce = Optional.ofNullable(l1.get(keyPrimary)).orElse(l1.get(keyAlias));
        if (ce == null) throw new IllegalStateException("No cache for key=" + keyPrimary + " (304/err without cache)");
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
    public record Payload(byte[] body, String revision, String etag, boolean fromNetwork) {}

    public List<ConfigEnvelope<String>> getItemsByBundle(Collection<String> rawKeys) {
        String csv = toCsvKeys(rawKeys);
        var resp = feign.bundleCsv(csv);
        return (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null)
                ? resp.getBody() : List.of();
    }

    /** CHỈNH Ở ĐÂY: chuẩn hoá “leaf” thành absolute key đúng gốc */
    private static String toCsvKeys(Collection<String> rawKeys) {
        if (rawKeys == null || rawKeys.isEmpty()) return "";
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String k : rawKeys) {
            if (k == null || k.isBlank()) continue;
            String v = k.trim();
            if (v.startsWith("/")) v = v.substring(1);

            if (v.startsWith("config/")) {
                // đã tuyệt đối -> giữ nguyên
                normalized.add(v);
            } else if (v.startsWith("logicconfig/")) {
                // logicconfig shorthand -> thêm "config/"
                normalized.add("config/" + v);
            } else {
                // leaf item shorthand -> ép vào gameworld/item
                // ví dụ: "block_item" -> "config/gameworld/item/block_item"
                normalized.add("config/gameworld/item/" + v);
            }
        }
        return String.join(",", normalized);
    }
}