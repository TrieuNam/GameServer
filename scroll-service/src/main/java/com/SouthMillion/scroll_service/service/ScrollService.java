package com.SouthMillion.scroll_service.service;
import com.SouthMillion.scroll_service.entity.*;
import com.SouthMillion.scroll_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Slf4j @Service @RequiredArgsConstructor
public class ScrollService {
    private final ScrollMetaRepository metaRepo;
    private final ScrollItemRepository itemRepo;

    /** Configurable item ID pool for scroll draws. Override in application.yml:
     *  scroll.item-pool=2001,2002,2003,2010,2020 */
    @Value("${scroll.item-pool:1001,1002,1003,1004,1005,1006,1007,1008,1009,1010}")
    private String[] itemPoolConfig;

    private final Random random = new Random();

    /** Pick a random itemId from the configured item pool. */
    private int pickItemId() {
        if (itemPoolConfig == null || itemPoolConfig.length == 0) return 1001;
        String raw = itemPoolConfig[random.nextInt(itemPoolConfig.length)].trim();
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 1001; }
    }

    public ScrollMeta getMeta(Long roleId) {
        return metaRepo.findByRoleId(roleId)
                .orElseGet(() -> metaRepo.save(
                        ScrollMeta.builder().roleId(roleId).freeNum(1).baoDiNum(0).build()));
    }

    public List<ScrollItem> getList(Long roleId) {
        return itemRepo.findByRoleId(roleId);
    }

    @Transactional
    public Map<String, Object> draw(Long roleId, int count) {
        ScrollMeta meta = getMeta(roleId);
        List<ScrollItem> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            meta.setBaoDiNum(meta.getBaoDiNum() + 1);
            int itemId = pickItemId(); // from configured item pool
            List<ScrollItem> existing = itemRepo.findByRoleId(roleId);
            ScrollItem item = ScrollItem.builder()
                    .roleId(roleId).scrollIndex(existing.size())
                    .itemId(itemId).level(1).wearMark(0).param(0).build();
            itemRepo.save(item);
            drawn.add(item);
        }
        metaRepo.save(meta);
        return Map.of("success", true, "drawn", drawn, "meta", meta);
    }
}
