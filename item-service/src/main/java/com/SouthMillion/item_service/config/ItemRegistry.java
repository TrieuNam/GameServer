package com.SouthMillion.item_service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemMeta;
import org.SouthMillion.dto.item.ItemType;
import org.SouthMillion.dto.item.RawItemRow;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ItemRegistry {

    private final ConfigClient cfg;
    private final ObjectMapper om = new ObjectMapper();

    private final Map<Integer, ItemMeta> metaById = new ConcurrentHashMap<>();
    private final Map<Integer, RawItemRow> rawById  = new ConcurrentHashMap<>();

    private volatile String currentRevision = "";
    private final Cache<String, Boolean> parsedFile = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(512).build();

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /** Tự nạp khi app sẵn sàng (an toàn nếu ConfigClient cần context fully up) */
    @EventListener(ApplicationReadyEvent.class)
    public void onAppReady() {
        ensureLoaded();
    }

    /** Cho phép controller/services gọi để đảm bảo đã nạp */
    public void ensureLoaded() {
        if (initialized.get() && !metaById.isEmpty()) return;
        synchronized (this) {
            if (initialized.get() && !metaById.isEmpty()) return;
            warmup();                   // đã synchronized sẵn
            initialized.set(true);
        }
    }

    /** Dùng trong endpoint reload thủ công (tuỳ bạn có muốn hay không) */
    public synchronized void reload(boolean force) {
        parsedFile.invalidateAll();
        initialized.set(false);
        warmup();
        initialized.set(true);
    }

    public synchronized void warmup() {
        List<String> leaves = cfg.listItems();        // ví dụ ["other","equipment","gift",...]
        cfg.getItemsByBundle(leaves);                 // L1 cache trong ConfigClient

        for (String leaf : leaves) {
            if (leaf.endsWith("~")) continue;         // bỏ file backup
            loadItemLeaf(leaf);
        }

        // nạp các file logic *_item.json cần thiết
        safeLoadLogic("logicconfig/bag_cfg.json");
        // safeLoadLogic("logicconfig/gem_item.json");
        // safeLoadLogic("logicconfig/scroll_item.json");
    }

    public Optional<ItemMeta> meta(int id) {
        ensureLoaded();                                // <- quan trọng
        return Optional.ofNullable(metaById.get(id));
    }
    public Optional<RawItemRow> raw(int id)  {
        ensureLoaded();
        return Optional.ofNullable(rawById.get(id));
    }
    public String revision() { return currentRevision; }
    public int size() { return metaById.size(); }

    // ====== helpers ======

    private void loadItemLeaf(String leaf) {
        var payload = cfg.getItemLeaf(leaf);
        if (payload.fromNetwork()) {
            parseAndIndex(leaf, payload.body());
            currentRevision = payload.revision();
            parsedFile.put("item/"+leaf, true);
        } else if (parsedFile.getIfPresent("item/"+leaf) == null) {
            parseAndIndex(leaf, payload.body());
            currentRevision = payload.revision();
            parsedFile.put("item/"+leaf, true);
        }
    }

    private void safeLoadLogic(String logicLeaf) {
        try {
            var p = cfg.getLogicLeaf(logicLeaf);
            // parse phụ trợ (nếu cần) ở đây
        } catch (Exception ignore) {}
    }

    private void parseAndIndex(String sourceLeaf, byte[] body) {
        try {
            JsonNode root = om.readTree(body);
            if (root.isArray()) {
                for (JsonNode node : root) addRow(sourceLeaf, "root", node);
            } else if (root.isObject()) {
                var fns = root.fieldNames();
                while (fns.hasNext()) {
                    String top = fns.next();
                    JsonNode arr = root.get(top);
                    if (arr != null && arr.isArray()) {
                        for (JsonNode row : arr) addRow(sourceLeaf, top, row);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parse item file '" + sourceLeaf + "' failed", e);
        }
    }

    private void addRow(String sourceLeaf, String topNode, JsonNode row) {
        int id = asInt(row.get("id"), -1);
        if (id <= 0) return;

        String name = asText(row.get("name"), "");
        int itemTypeCode = asInt(row.get("item_type"), 0);
        long pileLimit = asLong(row.get("pile_limit"), 0L);
        boolean virtualItem = asInt(row.get("is_virtual"), 0) == 1;
        long sell = asLong(row.get("sellprice"), 0L);
        long invalid = asLong(row.get("invalid_time"), 0L);

        var meta = new ItemMeta(
                id, name, ItemType.fromCode(itemTypeCode),
                virtualItem, (int) pileLimit, (int) sell, invalid,
                topNode, sourceLeaf
        );
        metaById.put(id, meta);

        Map<String,String> fields = new HashMap<>();
        row.fieldNames().forEachRemaining(fn -> {
            var v = row.get(fn);
            fields.put(fn, (v == null || v.isNull()) ? null : v.asText());
        });
        rawById.put(id, new RawItemRow(id, topNode, sourceLeaf, fields));
    }

    private static int asInt(JsonNode n, int d){
        try { return (n==null||n.isNull())? d : Integer.parseInt(n.asText().trim()); }
        catch (Exception e){ return d; }
    }
    private static long asLong(JsonNode n, long d){
        try { return (n==null||n.isNull())? d : Long.parseLong(n.asText().trim()); }
        catch (Exception e){ return d; }
    }
    private static String asText(JsonNode n, String d){ return (n==null||n.isNull()) ? d : n.asText(); }
}