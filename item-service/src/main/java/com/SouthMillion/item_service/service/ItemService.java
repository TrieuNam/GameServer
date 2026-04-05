package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.config.ItemCache;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ItemService {

    private final ItemCache cache;

    public ItemService(ItemCache cache) {
        this.cache = cache;
    }

    public ItemMetaDTO meta(int itemId) { return cache.getOrLoad(itemId); }

    public Map<Integer, ItemMetaDTO> batch(List<Integer> ids) {
        Map<Integer, ItemMetaDTO> out = new LinkedHashMap<>();
        List<Integer> missingIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();

        if (ids == null || ids.isEmpty()) {
            return out;
        }

        log.debug("[item.meta.batch] request ids={} size={}", ids, ids == null ? 0 : ids.size());
        for (Integer rawId : ids) {
            if (rawId == null) {
                continue;
            }
            int id = rawId;
            try {
                out.put(id, cache.getOrLoad(id));
            } catch (ItemCache.ItemNotFoundException ignored) {
                // Reserved/system ids like 1 (gold) may not have item meta and should not spam warnings.
                if (id <= 1) {
                    log.debug("[item.meta.batch] skip reserved/system itemId={}", id);
                    continue;
                }
                // Best effort for batch API: skip ids that are temporarily unavailable.
                missingIds.add(id);
            } catch (Exception ex) {
                failedIds.add(id);
                log.warn("[item.meta.batch] failed itemId={} ex={}", id, ex.toString());
            }
        }

        if (!missingIds.isEmpty()) {
            log.warn("[item.meta.batch] missing item ids={} (returned {} / {})",
                    missingIds, out.size(), ids.size());
        }
        if (!failedIds.isEmpty()) {
            log.warn("[item.meta.batch] failed item ids={} (returned {} / {})",
                    failedIds, out.size(), ids.size());
        }
        if (missingIds.isEmpty() && failedIds.isEmpty()) {
            log.debug("[item.meta.batch] all ids resolved, returned={}", out.size());
        }
        return out;
    }

    public String typeOf(int itemId) { return meta(itemId).itemType(); }

    public boolean exists(int itemId) {
        try { cache.getOrLoad(itemId); return true; }
        catch (ItemCache.ItemNotFoundException e) { return false; }
    }

    public boolean validStack(int itemId, int count) {
        var m = meta(itemId);
        Integer pile = m.pileLimit();
        if (pile == null || pile <= 0) return false;
        return count >= 1 && count <= pile;
    }

    // ===== Item Recycle =====
    public Map<String, Object> recycleItems(Long roleId, List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", "No items provided");
            return result;
        }

        List<Integer> validIds = new ArrayList<>();
        List<Integer> invalidIds = new ArrayList<>();
        long totalRewardGold = 0L;

        for (Integer itemId : itemIds) {
            try {
                ItemMetaDTO meta = cache.getOrLoad(itemId);
                validIds.add(itemId);
                // Reward gold based on sellPrice from item config, fallback to quality * 50
                if (meta.sellPrice() != null && meta.sellPrice() > 0) {
                    totalRewardGold += meta.sellPrice();
                } else if (meta.quality() != null && meta.quality() > 0) {
                    totalRewardGold += meta.quality() * 50L;
                } else {
                    totalRewardGold += 100L;
                }
            } catch (ItemCache.ItemNotFoundException e) {
                invalidIds.add(itemId);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (!invalidIds.isEmpty()) {
            result.put("success", false);
            result.put("error", "Invalid item IDs: " + invalidIds);
            result.put("invalidIds", invalidIds);
            return result;
        }

        // NOTE: Actual item consumption (deduct from bag) and currency grant (wallet) should
        // be performed by the caller (e.g., bag-service) which owns the player inventory.
        result.put("success", true);
        result.put("recycledCount", validIds.size());
        result.put("rewardGold", totalRewardGold);
        result.put("recycledItemIds", validIds);
        return result;
    }
}
