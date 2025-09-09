package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.config.ItemCache;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemService {

    private final ItemCache cache;

    public ItemService(ItemCache cache) {
        this.cache = cache;
    }

    public ItemMetaDTO meta(int itemId) { return cache.getOrLoad(itemId); }

    public Map<Integer, ItemMetaDTO> batch(List<Integer> ids) {
        Map<Integer, ItemMetaDTO> out = new LinkedHashMap<>();
        for (int id : ids) out.put(id, cache.getOrLoad(id));
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
}
