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
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ====== Query ======
    @Cacheable(cacheNames = "bag:items", key = "#roleId")
    public List<BagDTOs.ItemView> list(Long roleId) {
        return repo.findAllByRoleId(roleId).stream()
                .map(ItemViewMapper::from)
                .collect(Collectors.toList());
    }

    // ====== Grant/Add ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public List<BagDTOs.ItemView> grant(String userId, Long roleId,
                                        List<BagDTOs.GrantItem> items, String eventId) {
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

        }
        return out;
    }


    // ====== Use/Consume ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public void use(Long roleId, BagDTOs.UseItemReq req) {
        long requested = req.getNum() == null ? 0L : req.getNum().longValue();
        log.info("[bag.use] roleId={} itemId={} requested={}", roleId, req.getItemId(), requested);
        if (requested <= 0) {
            throw new IllegalStateException("Số lượng sử dụng phải lớn hơn 0.");
        }
        long newNum = consumeExactly(roleId, req.getItemId(), requested);
        if (newNum < 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm hoặc không tồn tại.");
        }
        log.info("[bag.use] roleId={} itemId={} requested={} remaining={}",
                roleId, req.getItemId(), requested, newNum);
    }

    // ====== Sell/Discard ======
    @Transactional
    @CacheEvict(cacheNames = "bag:items", key = "#roleId")
    public BagDTOs.SellResult sell(Long roleId, BagDTOs.SellItemReq req) {
        long requested = req.getNum() == null ? 0L : req.getNum().longValue();
        log.info("[bag.sell] roleId={} itemId={} requested={} unitPrice={}",
                roleId, req.getItemId(), requested, req.getUnitPrice());
        if (requested <= 0) {
            throw new IllegalStateException("Số lượng bán phải lớn hơn 0.");
        }
        long newNum = consumeExactly(roleId, req.getItemId(), requested);
        if (newNum < 0) {
            throw new IllegalStateException("Không đủ số lượng vật phẩm để bán.");
        }
        long gold = Optional.ofNullable(req.getUnitPrice()).orElse(0L) * requested;
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
                long newNum = consumeExactly(roleId, itemId, 1);
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

    /**
     * Trả về level và exp hiện tại của recycling — không thay đổi dữ liệu.
     * Dùng cho push bootstrap (1686).
     */
    public Map<String, Object> getRecycleProgress(Long roleId) {
        RecycleProgress p = getOrCreateRecycle(roleId);
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
        return repo.findAllByRoleIdAndItemId(roleId, itemId).stream()
                .mapToLong(item -> item.getNum() == null ? 0L : item.getNum())
                .sum();
    }

    private long consumeExactly(Long roleId, Integer itemId, long requested) {
        if (itemId == null || requested <= 0) {
            log.warn("[bag.consume.exact] invalid request roleId={} itemId={} requested={}",
                    roleId, itemId, requested);
            return -1L;
        }
        List<BagItem> stacks = repo.findAllByRoleIdAndItemIdForUpdate(roleId, itemId);
        long total = stacks.stream().mapToLong(it -> it.getNum() == null ? 0L : it.getNum()).sum();
        log.info("[bag.consume.exact] roleId={} itemId={} requested={} stackCount={} totalBefore={}",
                roleId, itemId, requested, stacks.size(), total);
        if (total < requested) {
            log.warn("[bag.consume.exact] insufficient roleId={} itemId={} requested={} totalBefore={} stackCount={}",
                    roleId, itemId, requested, total, stacks.size());
            return -1L;
        }

        long remaining = requested;
        for (BagItem stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            long current = stack.getNum() == null ? 0L : stack.getNum();
            if (current <= 0) {
                continue;
            }
            long take = Math.min(current, remaining);
            long after = current - take;
            log.info("[bag.consume.exact] roleId={} itemId={} stackId={} before={} take={} after={}",
                    roleId, itemId, stack.getId(), current, take, after);
            if (after <= 0) {
                repo.delete(stack);
            } else {
                stack.setNum(after);
                repo.save(stack);
            }
            remaining -= take;
        }

        if (remaining > 0) {
            log.error("[bag.consume.exact] interrupted roleId={} itemId={} requested={} remaining={}",
                    roleId, itemId, requested, remaining);
            throw new IllegalStateException("Consume interrupted due to concurrent update.");
        }
        long currentNum = currentNum(roleId, itemId);
        log.info("[bag.consume.exact] done roleId={} itemId={} requested={} totalAfter={}",
                roleId, itemId, requested, currentNum);
        return currentNum;
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