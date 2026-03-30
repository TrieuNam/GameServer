package com.SouthMillion.bag_service.service;

import com.SouthMillion.bag_service.client.WalletFeign;
import com.SouthMillion.bag_service.enity.BagItem;
import com.SouthMillion.bag_service.enity.RecycleProgress;
import com.SouthMillion.bag_service.mapper.ItemViewMapper;
import com.SouthMillion.bag_service.repository.BagEventDedupRepository;
import com.SouthMillion.bag_service.repository.BagItemRepository;
import com.SouthMillion.bag_service.repository.RecycleProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.event.bag.BagChangedEvent;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BagDomainService {

    private final BagItemRepository repo;
    private final BagEventDedupRepository dedupRepo;
    private final RecycleProgressRepository recycleRepo;

    @Autowired(required = false)
    private WalletFeign walletFeign;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.bag-changed-topic:gameh5.bag.changed}")
    private String bagChangedTopic;

    // ====== Query ======
    @Cacheable(cacheNames = "bag:items", key = "#roleId")
    public List<BagDTOs.ItemView> list(Long roleId) {
        return repo.findAllByRoleId(roleId).stream()
                .map(ItemViewMapper::from)
                .collect(Collectors.toList());
    }

    // ====== Grant/Add ======
    @Transactional
    public List<BagDTOs.ItemView> grant(String userId, Long roleId,
                                        List<BagDTOs.GrantItem> items, String eventId) {
        return grant(userId, roleId, items, eventId, true);
    }

    @Transactional
    public List<BagDTOs.ItemView> grant(String userId, Long roleId,
                                        List<BagDTOs.GrantItem> items, String eventId,
                                        boolean publishChangedEvent) {
        if (!dedupRepo.insertIgnore(eventId)) {
            log.info("Grant ignored (idempotent): eventId={}", eventId);
            return List.of();
        }

        List<BagDTOs.ItemView> out = new ArrayList<>(items.size());

        for (var i : items) {
            long delta = i.getNum() == null ? 1L : i.getNum().longValue();
            boolean bind = i.getBind() != null && i.getBind() != 0;
            int quality = i.getQuality() == null ? 1 : i.getQuality();
            int bagType = i.getBagType() == null ? 0 : i.getBagType();

            BagItem bi = repo.findExact(roleId, i.getItemId(), bind, quality, bagType, i.getExpireAt())
                    .map(existing -> {
                        existing.setNum((existing.getNum() == null ? 0L : existing.getNum()) + delta);
                        existing.setUserId(userId);
                        existing.setQuality(quality);
                        existing.setBagType(bagType);
                        return repo.save(existing);
                    })
                    .orElseGet(() -> repo.save(BagItem.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .roleId(roleId)
                            .itemId(i.getItemId())
                            .num(delta)
                            .bind(bind)
                            .quality(quality)
                            .bagType(bagType)
                            .expireAt(i.getExpireAt())
                            .build()));

            out.add(BagDTOs.ItemView.builder()
                    .roleId(String.valueOf(bi.getRoleId()))
                    .itemId(bi.getItemId())
                    .num(bi.getNum() != null ? bi.getNum().intValue() : null)
                    .bind(bi.getBind() != null && bi.getBind() ? 1 : 0)
                    .expireAt(bi.getExpireAt())
                    .quality(bi.getQuality())
                    .bagType(bi.getBagType())
                    .build());

            if (publishChangedEvent) {
                publishBagChanged(eventId, userId, roleId, bi.getItemId(), delta,
                        bi.getNum() == null ? 0L : bi.getNum(), "grant");
            }
        }
        return out;
    }


    // ====== Use/Consume ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public void use(Long roleId, BagDTOs.UseItemReq req) {
        int changed = repo.consume(roleId, req.getItemId(), req.getNum());
        if (changed <= 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm hoặc không tồn tại.");
        }
        repo.cleanupZero(roleId, req.getItemId());
        long newNum = currentNum(roleId, req.getItemId());
        publishBagChanged(UUID.randomUUID().toString(), String.valueOf(roleId), roleId,
                req.getItemId(), -req.getNum().longValue(), newNum, "use");
    }

    // ====== Sell/Discard ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public BagDTOs.SellResult sell(Long roleId, BagDTOs.SellItemReq req) {
        int changed = repo.consume(roleId, req.getItemId(), req.getNum());
        if (changed <= 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm để bán.");
        }
        repo.cleanupZero(roleId, req.getItemId());
        long newNum = currentNum(roleId, req.getItemId());
        long gold = Optional.ofNullable(req.getUnitPrice()).orElse(0L) * req.getNum();
        // Credit gold to wallet after selling items
        if (gold > 0 && walletFeign != null) {
            try {
                WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                        .roleId(String.valueOf(roleId))
                        .changes(List.of(WalletDTOs.Change.builder().itemId(1L).amount(gold).build()))
                        .reason(201) // 201 = sell item
                        .build();
                walletFeign.batchAdd(walletReq);
            } catch (Exception e) {
                log.warn("[bag] Failed to credit wallet after sell for roleId={}: {}", roleId, e.getMessage());
            }
        }
        publishBagChanged(UUID.randomUUID().toString(), String.valueOf(roleId), roleId,
                req.getItemId(), -req.getNum().longValue(), newNum, "sell");
        return new BagDTOs.SellResult(gold, req.getNum());
    }

    // ====== Item Recycle (物品回收) ======
    private static final long EXP_PER_ITEM  = 10L;   // exp gained per recycled item
    private static final long EXP_PER_LEVEL = 100L;  // exp needed per level

    /**
     * Recycle a list of items: consume them from the bag, award exp, level up if threshold met.
     * Returns map with "level" and "exp" fields matching PB_SCItemRecycleInfo.
     */
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public Map<String, Object> recycleItems(Long roleId, List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            RecycleProgress p = getOrCreateRecycle(roleId);
            return buildRecycleResult(p);
        }

        // Consume each item (1 unit each); skip if not present
        for (Integer itemId : itemIds) {
            try {
                int changed = repo.consume(roleId, itemId, 1);
                if (changed > 0) {
                    repo.cleanupZero(roleId, itemId);
                    publishBagChanged(UUID.randomUUID().toString(), String.valueOf(roleId), roleId,
                            itemId, -1L, currentNum(roleId, itemId), "recycle");
                }
            } catch (Exception e) {
                log.warn("[Recycle] Could not consume itemId={} for role={}: {}", itemId, roleId, e.getMessage());
            }
        }

        // Award exp and level up
        RecycleProgress p = getOrCreateRecycle(roleId);
        long gained = (long) itemIds.size() * EXP_PER_ITEM;
        p.setExp(p.getExp() + gained);
        while (p.getExp() >= EXP_PER_LEVEL) {
            p.setExp(p.getExp() - EXP_PER_LEVEL);
            p.setLevel(p.getLevel() + 1);
        }
        recycleRepo.save(p);

        log.info("[Recycle] roleId={} recycled {} items, gained {} exp, now level={} exp={}",
                roleId, itemIds.size(), gained, p.getLevel(), p.getExp());
        return buildRecycleResult(p);
    }

    private RecycleProgress getOrCreateRecycle(Long roleId) {
        return recycleRepo.findByRoleId(roleId).orElseGet(() ->
                recycleRepo.save(RecycleProgress.builder().roleId(roleId).level(1).exp(0L).build()));
    }

    private Map<String, Object> buildRecycleResult(RecycleProgress p) {
        Map<String, Object> result = new HashMap<>();
        result.put("level", p.getLevel());
        result.put("exp", p.getExp());
        result.put("success", true);
        return result;
    }

    private long currentNum(Long roleId, Integer itemId) {
        return repo.findAllByRoleId(roleId).stream()
                .filter(item -> itemId != null && itemId.equals(item.getItemId()))
                .mapToLong(item -> item.getNum() == null ? 0L : item.getNum())
                .sum();
    }

    private void publishBagChanged(String eventId, String userId, Long roleId,
                                   Integer itemId, Long delta, Long newNum, String reason) {
        if (kafkaTemplate == null || itemId == null) return;
        try {
            BagChangedEvent event = BagChangedEvent.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .roleId(String.valueOf(roleId))
                    .itemId(itemId)
                    .delta(delta)
                    .newNum(newNum)
                    .reason(reason)
                    .at(Instant.now())
                    .build();
            kafkaTemplate.send(bagChangedTopic, String.valueOf(roleId), event);
        } catch (Exception e) {
            log.warn("[bag] Failed to publish bag.changed event reason={} roleId={} itemId={}: {}",
                    reason, roleId, itemId, e.getMessage());
        }
    }
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public BagDTOs.SellResult buyCmd(Long roleId, BagDTOs.BuyCmdReq req) {
        long totalCost = (long) req.getPrice() * Math.max(1, req.getNum());
        if (totalCost > 0 && walletFeign != null) {
            try {
                WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                        .roleId(String.valueOf(roleId))
                        .changes(List.of(WalletDTOs.Change.builder()
                                .itemId((long) Math.max(1, req.getCurrency()))
                                .amount(-totalCost)
                                .build()))
                        .reason(101)
                        .build();
                walletFeign.batchAdd(walletReq);
            } catch (Exception e) {
                log.warn("[bag] buyCmd wallet deduct failed for roleId={}: {}", roleId, e.getMessage());
                throw new IllegalStateException("Insufficient currency for buy-cmd.");
            }
        }
        if (req.getItemId() > 0) {
            String eventId = UUID.randomUUID().toString();
            grant("system", roleId, List.of(BagDTOs.GrantItem.builder()
                    .itemId(req.getItemId()).num(Math.max(1, req.getNum())).build()), eventId);
        }
        return new BagDTOs.SellResult(totalCost, Math.max(1, req.getNum()));
    }
}