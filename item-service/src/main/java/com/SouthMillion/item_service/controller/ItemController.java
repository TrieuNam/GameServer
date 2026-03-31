package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.config.ItemCache;
import com.SouthMillion.item_service.service.ItemService;
import com.fasterxml.jackson.databind.JsonNode;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.SouthMillion.dto.item.ValidateResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ItemController {

    private final ItemService svc;
    private final ItemCache cache;

    public ItemController(ItemService svc, ItemCache cache) {
        this.svc = svc; this.cache = cache;
    }

    @GetMapping("/api/item/meta")
    public ResponseEntity<ItemMetaDTO> meta(@RequestParam int itemId) {
        try {
            return ResponseEntity.ok(svc.meta(itemId));
        } catch (ItemCache.ItemNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/item/meta/batch")
    public ResponseEntity<Map<Integer, ItemMetaDTO>> batch(@RequestParam("itemId") List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(svc.batch(itemIds));
    }

    @GetMapping("/api/item/type")
    public ResponseEntity<Map<String, Object>> type(@RequestParam int itemId) {
        return ResponseEntity.ok(Map.of("itemId", itemId, "itemType", svc.typeOf(itemId)));
    }

    @GetMapping("/api/item/validate")
    public ResponseEntity<ValidateResp> validate(@RequestParam int itemId, @RequestParam int count) {
        if (count < 1) return ResponseEntity.badRequest().body(new ValidateResp(false, "count must be >= 1"));
        if (!svc.exists(itemId)) return ResponseEntity.status(404).body(new ValidateResp(false, "item not found"));
        boolean ok = svc.validStack(itemId, count);
        return ResponseEntity.ok(new ValidateResp(ok, ok ? "OK" : "exceed pile_limit"));
    }

    // internal debug (hạn chế expose prod)
    @GetMapping("/internal/item/meta/raw")
    public ResponseEntity<JsonNode> raw(@RequestParam("path") String pathOrId) {
        try {
            int itemId = Integer.parseInt(pathOrId);
            return ResponseEntity.ok(cache.getRaw(itemId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/internal/item/meta/cache")
    public ResponseEntity<Map<String, Object>> evictMeta(@RequestParam("itemId") int itemId) {
        boolean redisDeleted = cache.evictMeta(itemId);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "itemId", itemId,
                "redisDeleted", redisDeleted
        ));
    }

    @DeleteMapping("/internal/item/meta/cache/all")
    public ResponseEntity<Map<String, Object>> evictAllMeta() {
        int removed = cache.evictAllMeta();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "removed", removed
        ));
    }

    // ===== Item Recycle =====
    @PostMapping("/api/item/{roleId}/recycle")
    public ResponseEntity<Map<String, Object>> recycleItems(
            @PathVariable Long roleId,
            @RequestBody List<Integer> itemIds) {
        return ResponseEntity.ok(svc.recycleItems(roleId, itemIds));
    }
}