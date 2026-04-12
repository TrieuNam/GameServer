package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.LimitCoreConfigCache;
import com.SouthMillion.role_service.config.LimitCoreConfigCache.CoreEntry;
import com.SouthMillion.role_service.config.LimitCoreConfigCache.CoreboxEntry;
import com.SouthMillion.role_service.entity.PlayerLimitCore;
import com.SouthMillion.role_service.repository.PlayerLimitCoreRepository;
import com.SouthMillion.role_service.service.client.BagFeign;
import com.SouthMillion.role_service.service.client.LimitCoreItemFeign;
import com.SouthMillion.role_service.service.client.WalletFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Business logic for CoreCrisis / LimitCore (限界突破).
 *
 * <p>Operations dispatched from OtherHandler msgId 1467:
 * <ul>
 *   <li>type=0 LEVEL_UP: consume chips → increment level → return all 6 levels</li>
 *   <li>type=1 DRAW: spend diamonds → randomly draw 3 chips → grant to bag
 *       → return all 6 levels + drawn item list</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimitCoreService {

    private static final int OP_LEVEL_UP = 0;
    private static final int OP_DRAW     = 1;

    /** Number of core types (1–6). */
    private static final int NUM_TYPES = 6;

    /** Virtual currency type string for paid diamonds */
    private static final String CURRENCY_PAID_GOLD = "paid_gold";

    private final PlayerLimitCoreRepository repo;
    private final LimitCoreConfigCache       cfg;
    private final LimitCoreItemFeign         itemFeign;
    private final WalletFeign                walletFeign;
    private final BagFeign                   bagFeign;

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns all 6 core levels for the player (default 0 for unstarted types).
     * Index 0 = limit_type 1 (Mount), …, index 5 = limit_type 6 (ShenQi).
     */
    public List<Integer> getAllLevels(Long roleId) {
        Map<Integer, Integer> levelMap = buildLevelMap(roleId);
        List<Integer> result = new ArrayList<>(NUM_TYPES);
        for (int t = 1; t <= NUM_TYPES; t++) {
            result.add(levelMap.getOrDefault(t, 0));
        }
        return result;
    }

    /**
     * Dispatch handler for PB_CSLimitCoreReq.
     *
     * @return map always containing {@code coreLevels}; DRAW additionally contains {@code drawnItems}
     */
    public Map<String, Object> handleRequest(Long roleId, int type, int p1) {
        if (type == OP_LEVEL_UP) {
            return doLevelUp(roleId, p1);
        } else if (type == OP_DRAW) {
            return doDraw(roleId, p1);
        } else {
            log.warn("[LimitCore] Unknown op type={} roleId={}", type, roleId);
            return buildResult(roleId, null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // LEVEL UP
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> doLevelUp(Long roleId, int limitType) {
        if (limitType < 1 || limitType > NUM_TYPES) {
            log.warn("[LimitCore] Invalid limitType={} roleId={}", limitType, roleId);
            return buildResult(roleId, null);
        }

        PlayerLimitCore entity = repo.findByRoleIdAndLimitType(roleId, limitType)
                .orElseGet(() -> {
                    PlayerLimitCore e = new PlayerLimitCore();
                    e.setRoleId(roleId);
                    e.setLimitType(limitType);
                    e.setLevel(0);
                    return e;
                });

        int currentLevel = entity.getLevel();
        Optional<CoreEntry> cfgOpt = cfg.getCoreEntry(limitType, currentLevel);

        if (cfgOpt.isEmpty()) {
            log.warn("[LimitCore] No config for limitType={} level={} roleId={} — possibly at max",
                    limitType, currentLevel, roleId);
            return buildResult(roleId, null);
        }

        CoreEntry entry = cfgOpt.get();
        if (entry.needCoreNum() <= 0) {
            log.warn("[LimitCore] Already at max level limitType={} roleId={}", limitType, roleId);
            return buildResult(roleId, null);
        }

        // Check + consume chips
        String roleIdStr = String.valueOf(roleId);
        Boolean notEnough = itemFeign.isNotEnough(roleIdStr, entry.needItemId(), entry.needCoreNum());
        if (Boolean.TRUE.equals(notEnough)) {
            throw new RuntimeException("Không đủ chip để nâng cấp limitType=" + limitType);
        }
        Boolean consumed = itemFeign.consume(roleIdStr, entry.needItemId(), entry.needCoreNum());
        if (!Boolean.TRUE.equals(consumed)) {
            throw new RuntimeException("Trừ chip thất bại limitType=" + limitType);
        }

        // Increment level
        entity.setLevel(currentLevel + 1);
        repo.save(entity);

        log.info("[LimitCore] LEVEL_UP roleId={} limitType={} level {} → {}",
                roleId, limitType, currentLevel, entity.getLevel());
        return buildResult(roleId, null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // DRAW BOX
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> doDraw(Long roleId, int boxType) {
        if (boxType < 0 || boxType > 2) {
            log.warn("[LimitCore] Invalid boxType={} roleId={}", boxType, roleId);
            return buildResult(roleId, null);
        }

        String roleIdStr = String.valueOf(roleId);

        // Deduct diamonds for paid box types
        if (boxType == 1) {
            deductDiamonds(roleIdStr, cfg.getPrice1(), "limitcore-box1");
        } else if (boxType == 2) {
            deductDiamonds(roleIdStr, cfg.getPrice2(), "limitcore-box2");
        }

        // Roll chips
        List<CoreboxEntry> pool = cfg.getCoreboxEntries(boxType);
        if (pool.isEmpty()) {
            log.warn("[LimitCore] No corebox config for boxType={}", boxType);
            return buildResult(roleId, null);
        }

        int numToDraw = Math.max(1, cfg.getRewardNum());
        List<DrawnItem> drawn = weightedDraw(pool, numToDraw);

        // Grant items to bag
        List<BagAddItemReq.Item> bagItems = new ArrayList<>();
        for (DrawnItem d : drawn) {
            bagItems.add(BagAddItemReq.Item.builder()
                    .itemId(d.itemId())
                    .amount(d.num())
                    .bound(false)
                    .build());
        }
        try {
            bagFeign.add(BagAddItemReq.builder()
                    .userId(0L)
                    .roleId(roleId)
                    .items(bagItems)
                    .source("limitcore-draw")
                    .idemKey("lcdraw-" + roleId + "-" + System.currentTimeMillis())
                    .build());
        } catch (Exception e) {
            log.error("[LimitCore] Failed to grant items to bag roleId={}: {}", roleId, e.getMessage());
            throw new RuntimeException("Không thể cộng chip vào túi đồ");
        }

        log.info("[LimitCore] DRAW roleId={} boxType={} drawn={}", roleId, boxType, drawn);

        // Build drawn item list for response
        List<Map<String, Object>> drawnList = new ArrayList<>();
        for (DrawnItem d : drawn) {
            drawnList.add(Map.of("itemId", d.itemId(), "num", d.num()));
        }
        return buildResult(roleId, drawnList);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildResult(Long roleId, List<Map<String, Object>> drawnItems) {
        Map<String, Object> result = new HashMap<>();
        result.put("coreLevels", getAllLevels(roleId));
        if (drawnItems != null) {
            result.put("drawnItems", drawnItems);
        }
        return result;
    }

    private Map<Integer, Integer> buildLevelMap(Long roleId) {
        Map<Integer, Integer> map = new HashMap<>();
        for (PlayerLimitCore e : repo.findByRoleId(roleId)) {
            map.put(e.getLimitType(), e.getLevel());
        }
        return map;
    }

    private void deductDiamonds(String roleId, int amount, String reason) {
        Boolean hasEnough = walletFeign.hasEnough(roleId, CURRENCY_PAID_GOLD, (long) amount);
        if (!Boolean.TRUE.equals(hasEnough)) {
            throw new RuntimeException("Không đủ kim cương để mở hộp");
        }
        walletFeign.deductCurrency(Map.of(
                "roleId", roleId,
                "currencyType", CURRENCY_PAID_GOLD,
                "amount", (long) amount,
                "reason", reason
        ));
    }

    /**
     * Weighted random draw of {@code count} distinct items from the pool.
     */
    private List<DrawnItem> weightedDraw(List<CoreboxEntry> pool, int count) {
        Random rng = new Random();
        List<DrawnItem> result = new ArrayList<>();
        List<CoreboxEntry> remaining = new ArrayList<>(pool);

        int toDraw = Math.min(count, remaining.size());
        for (int i = 0; i < toDraw; i++) {
            int totalRate = remaining.stream().mapToInt(CoreboxEntry::boxRate).sum();
            int roll = rng.nextInt(Math.max(1, totalRate));
            int cumulative = 0;
            CoreboxEntry picked = remaining.get(remaining.size() - 1); // fallback
            for (CoreboxEntry e : remaining) {
                cumulative += e.boxRate();
                if (roll < cumulative) {
                    picked = e;
                    break;
                }
            }
            int num = picked.boxItemMin() + rng.nextInt(Math.max(1, picked.boxItemMax() - picked.boxItemMin() + 1));
            result.add(new DrawnItem(picked.boxItem(), num));
            remaining.remove(picked);
        }
        return result;
    }

    private record DrawnItem(int itemId, int num) {}
}
