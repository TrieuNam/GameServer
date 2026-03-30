package com.SouthMillion.box_service.service;

import com.SouthMillion.box_service.config.EquipmentIndex;
import com.SouthMillion.box_service.config.LuckUnpackConfigCache;
import com.SouthMillion.box_service.config.UnpackConfigCache;
import com.SouthMillion.box_service.enity.BoxSetting;
import com.SouthMillion.box_service.enity.BoxState;
import com.SouthMillion.box_service.enity.LuckState;
import com.SouthMillion.box_service.repository.BoxCompareStateRepository;
import com.SouthMillion.box_service.repository.BoxSettingRepository;
import com.SouthMillion.box_service.repository.BoxStateRepository;
import com.SouthMillion.box_service.repository.LuckStateRepository;
import com.SouthMillion.box_service.service.client.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.bag.BagDTOs.ItemDelta;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BoxService: triển khai đúng gameplay C++:
 * - Open: chỉ tạo popup khi rơi Equip; không cộng EXP khi mở.
 * - Wear: món cũ trở thành pending; không reset cờ mở-5.
 * - Sell/Decompose: tính coin/exp/vật liệu qua equip-service; nhân 5 nếu phù hợp; clear pending & reset cờ.
 */
@Slf4j
@Service
public class BoxService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    // ========= DEPENDENCIES =========
    private final BoxStateRepository boxRepo;
    private final LuckStateRepository luckRepo;
    private final BoxSettingRepository settingRepo;
    private final BoxCompareStateRepository compareStateRepo;

    private final UnpackConfigCache unpackCfg;
    private final LuckUnpackConfigCache luckCfg;
    private final EquipmentIndex equipIdx;                // NO hard-code
    private final BagFeign bag;
    private final ItemMetaFeign itemFeign;                // meta để lọc virtual
    private final EquipFeign equipFeign;
    private final RoleFeign roleFeign;
    private final WalletFeign walletFeign;
    private final Counter compareMissingEquippedBeforeCounter;
    private final Counter compareIncompleteStateCounter;
    /** Arena ticket itemId to drop on box open. Set -1 (default) to disable. */
    @Value("${box.arena-ticket.item-id:-1}")
    private int arenaTicketItemId;

    /**
     * Fallback unpack item id khi config-service chưa trả về "other.unpack_item_id".
     * Set trong application.yml: box.unpack-item-id=1001 (hoặc id hộp của bạn).
     */
    @Value("${box.unpack-item-id:0}")
    private int unpackItemIdFallback;

    public BoxService(BoxStateRepository boxRepo,
                      LuckStateRepository luckRepo,
                      BoxSettingRepository settingRepo,
                      BoxCompareStateRepository compareStateRepo,
                      UnpackConfigCache unpackCfg,
                      LuckUnpackConfigCache luckCfg,
                      EquipmentIndex equipIdx,
                      BagFeign bag,
                      ItemMetaFeign itemFeign,
                      EquipFeign equipFeign,
                      RoleFeign roleFeign,
                      WalletFeign walletFeign,
                      MeterRegistry meterRegistry) {
        this.boxRepo = boxRepo;
        this.luckRepo = luckRepo;
        this.settingRepo = settingRepo;
        this.compareStateRepo = compareStateRepo;
        this.unpackCfg = unpackCfg;
        this.luckCfg = luckCfg;
        this.equipIdx = equipIdx;
        this.bag = bag;
        this.itemFeign = itemFeign;
        this.equipFeign = equipFeign;
        this.roleFeign = roleFeign;
        this.walletFeign = walletFeign;
        this.compareMissingEquippedBeforeCounter = meterRegistry.counter("box.compare.missing_equipped_before");
        this.compareIncompleteStateCounter = meterRegistry.counter("box.compare.incomplete_state");
    }

    // ========= CONSTANTS =========
    private static final byte BAG_COMMON = 0;
    private static final byte BAG_EQUIP = 1;

    private static final int SRC_MSG_BOX = 3000;
    private static final int SRC_OP_OPEN = 3001;
    private static final int SRC_OP_REWARD = 3002;
    private static final int SRC_OP_BUY = 3003;
    private static final int SRC_OP_LEVEL = 3004;
    private static final int SRC_OP_CONSUME = 3005;
    private static final String MSG_NOT_REACHED = "NOT_REACHED";
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ========= PUBLIC APIS =========

    public BoxDTOs.InfoResp info(Long roleId) {
        var s = getOrCreate(roleId);
        maybeCompleteLevelUp(s);
        maybeDailyReset(s);
        EquipDTOs.WearFromBoxItem activeItem = resolveWearItem(roleId, s);
        Map<String, Object> pending = activeItem != null ? activeItem.toPendingMap() : parsePendingJsonSafe(s.getPendingJson());

        return BoxDTOs.InfoResp.builder()
                .boxLevel(s.getBoxLevel())
                .boxBuyTimes(s.getBoxBuyTimes())
                .levelUpEndEpoch(s.getLevelUpEndEpoch())
                .levelFetchFlag(s.getLevelFetchFlag())
                .openBoxTotal(s.getOpenBoxTotal())
                .lastOpenIsFive(s.isLastOpenIsFive())
                .shiZhuangNum(getSafeIntField(s, "shiZhuangNum"))  // cột optional
                .arenaItemNum(getSafeIntField(s, "arenaItemNum"))  // cột optional
                .pending(pending)
                .compareState(null)
                .build();
    }

    /**
     * Open box:
     * - Nếu còn pending hoặc đang cooldown -> trả pending hiện tại (không consume).
     * - Nếu fixed step → fixed reward (nếu là equip => pending).
     * - Nếu fashion vẫn còn quota ngày → add thẳng (bonus) và kết thúc step.
     * - Còn lại: random equip → pending + bonus reward nếu có.
     */
    @Transactional
    public BoxDTOs.OpenResp open(BoxDTOs.OpenReq req) {
        Long roleId = Long.valueOf(req.getRoleId());
        int count = Math.max(1, Math.min(5, req.getCount()));
        boolean isFive = count >= 5;

        var s = getOrCreate(roleId);
        maybeCompleteLevelUp(s);
        maybeDailyReset(s);

        long nowSec = Instant.now().getEpochSecond();

        EquipDTOs.WearFromBoxItem activeWearItem = resolveWearItem(roleId, s);
        boolean hasPendingEquip = activeWearItem != null && activeWearItem.getItemId() != null && activeWearItem.getItemId() > 0;
        if (hasPendingEquip) {
            return BoxDTOs.OpenResp.builder()
                .pending(activeWearItem.toPendingMap())
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(List.of())
                    .build();
        }

        // Cooldown
        long last = getSafeLongField(s, "lastOpenEpoch");
        if (last > nowSec + 3600) {
            long fixed = last / 1000L;              // ms -> s
            if (fixed > nowSec + 2) fixed = nowSec + 2;
            setSafeLongField(s, "lastOpenEpoch", fixed);
            s = boxRepo.save(s);
            last = fixed;
        }
        if (last > nowSec) {
            return BoxDTOs.OpenResp.builder()
                    .pending(parsePendingJsonSafe(s.getPendingJson()))
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(List.of())
                    .build();
        }

        // Unpack config
        Map<String, String> other = firstOrEmpty(unpackCfg.other());
        int boxItemId = pInt(other.get("unpack_item_id"), 0);
        // Fallback sang @Value nếu config-service chưa load hoặc thiếu key
        if (boxItemId <= 0) boxItemId = unpackItemIdFallback;
        if (boxItemId <= 0) {
            log.error("[box] Bad config: other.unpack_item_id not configured (set box.unpack-item-id in application.yml)");
            return BoxDTOs.OpenResp.builder()
                    .pending(parsePendingJsonSafe(s.getPendingJson()))
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(List.of())
                    .build();
        }

        // Fixed step?
        int step = nextFixedStep(s.getOpenBoxTotal(), isFive);
        long need = (step > 0) ? step : (isFive ? 5L : 1L);

        // Consume box item
        BagConsumeReq consumeReq = BagConsumeReq.builder()
                .userId(1L) // audit field
                .roleId(roleId)
                .itemId(boxItemId)
                .amount((int) need)
                .source("BOX_OPEN")
                .build();
        bag.consume(consumeReq);

        List<Map<String, Object>> bonus = new ArrayList<>();

        // ===== 1) Fixed reward theo order
        if (step > 0) {
            int order = s.getOpenBoxTotal() + step;
            int fixedItemId = fixedItemForOrder(order).orElse(0);

            if (fixedItemId > 0) {
                if (equipIdx.isEquipId(fixedItemId)) {
                    int playerLevel = fetchPlayerLevel(roleId, req.getRoleLevel());
                    int equipLevel = rollEquipLevelByPlayerLevel(playerLevel);

                    // Lấy (part, quality) từ fixedItemId nếu có; nếu không có thì random part và default quality=1
                    int[] pq = equipIdx.findPQLById(fixedItemId)
                            .orElseGet(() -> new int[]{pickEquipPart(), 1});
                    int part = pq[0];
                    int quality = pq[1];

                    // Resolve id "ưu tiên" theo (part, quality, level)
                    int resolvedItemId = equipIdx.resolve(part, quality, equipLevel).orElse(fixedItemId);

                    Map<String, Object> pending = new LinkedHashMap<>();
                    pending.put("kind", "equip");
                    pending.put("quality", quality);
                    pending.put("equipLevel", equipLevel);
                    pending.put("rolledAt", nowSec);
                    pending.put("count", need);
                    pending.put("itemId", resolvedItemId);
                    pending.put("equipType", part);
                    pending.put("isNew", true);

                    // === dùng EquipmentIndex helpers
                    equipIdx.statsJsonOf(resolvedItemId).ifPresent(json -> pending.put("stats", json));
                    equipIdx.allFieldsCanonicalOf(resolvedItemId).ifPresent(c -> pending.put("equipMeta", c));
                    equipIdx.allFieldsRawStringsOf(resolvedItemId).ifPresent(r -> pending.put("equipMetaRaw", r));
                    // optional: id ở idxPref để debug chọn đúng entry chưa
                    Map<Integer, Map<Integer, Map<Integer, Integer>>> pref = equipIdx.getIdxPreferred();
                    Integer idxPrefId = Optional.ofNullable(pref.get(part))
                            .map(m -> m.get(quality))
                            .map(m -> m.get(equipLevel))
                            .orElse(null);
                    if (idxPrefId != null) pending.put("idxPrefId", idxPrefId);

                    BoxDTOs.EquipRolled newEquipFixed = buildEquipRolled(resolvedItemId, part);
                    putRolledSnapshot(pending, newEquipFixed);
                    s.setPendingJson(writePendingJson(pending));
                        CurrentEquipLookup lookupFixed = findCurrentEquipWithRetry(roleId, part);
                        String statusFixed = lookupFixed.isLookupFailed() ? "PENDING_COMPARE_INCOMPLETE" : "PENDING_COMPARE";
                        BoxDTOs.BoxCompareStateResp compareStateFixed = saveCompareState(
                            roleId, newEquipFixed, lookupFixed.getEquip(), quality, equipLevel, 1, "BOX_OPEN", statusFixed);

                        s.setOpenBoxTotal(s.getOpenBoxTotal() + (int) need);
                    s.setLastOpenIsFive(need == 5);
                    setSafeLongField(s, "lastOpenEpoch", nowSec + 1);
                    boxRepo.save(s);

                    rollArenaTicketIfAny(roleId, s, bonus, other);
                    return BoxDTOs.OpenResp.builder()
                            .pending(pending)
                            .openBoxTotal(s.getOpenBoxTotal())
                            .lastOpenIsFive(s.isLastOpenIsFive())
                            .bonusItems(bonus)
                            .isNew(1)
                            .openEquip(newEquipFixed)
                            .compareState(compareStateFixed)
                            .build();
                } else {
                    // Non-equip fixed → add vào túi thường
                    addNonVirtualItems(roleId,
                            List.of(new ItemDelta(fixedItemId, 1)),
                            BAG_COMMON, SRC_OP_REWARD);
                    bonus.add(bonusItem(fixedItemId, 1, "fixed"));
                }
            }

            s.setOpenBoxTotal(s.getOpenBoxTotal() + (int) need);
            s.setLastOpenIsFive(need == 5);
            setSafeLongField(s, "lastOpenEpoch", nowSec + 1);
            boxRepo.save(s);

            rollArenaTicketIfAny(roleId, s, bonus, other);

            return BoxDTOs.OpenResp.builder()
                    .pending(null)
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(bonus)
                    .build();
        }

        // ===== 2) thời trang theo quota ngày (giữ nguyên)
        boolean allowFashion = "1".equals(String.valueOf(other.getOrDefault("get_shizhuang", "1")));
        int maxSz = pInt(other.get("max_shizhuang"), 0);
        int curSz = getSafeIntField(s, "shiZhuangNum");
        if (allowFashion && maxSz > 0 && curSz < maxSz) {
            BoxState finalS = s;
            pickFashionId().ifPresent(fid -> {
                addNonVirtualItems(roleId,
                        List.of(new ItemDelta(fid, 1)),
                        BAG_COMMON, SRC_OP_REWARD);
                bonus.add(bonusItem(fid, 1, "fashion"));
                setSafeIntField(finalS, "shiZhuangNum", curSz + 1);
            });

            s.setOpenBoxTotal(s.getOpenBoxTotal() + (int) need);
            s.setLastOpenIsFive(need == 5);
            setSafeLongField(s, "lastOpenEpoch", nowSec + 1);
            boxRepo.save(s);

            rollArenaTicketIfAny(roleId, s, bonus, other);

            return BoxDTOs.OpenResp.builder()
                    .pending(null)
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(bonus)
                    .build();
        }

        // ===== 3) RANDOM EQUIP -> pending
        Map<String, Object> colorRow = colorRowByLevel(s.getBoxLevel());
        int playerLevel = fetchPlayerLevel(roleId, req.getRoleLevel());
        int equipLevel = rollEquipLevelByPlayerLevel(playerLevel);
        int quality = rollQuality(colorRow, isFive);

        int part = pickEquipPart(); // theo part tồn tại trong equipment.json
        int itemId = resolveItemId(part, quality, equipLevel);

        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("kind", "equip");
        pending.put("quality", quality);
        pending.put("equipLevel", equipLevel);
        pending.put("rolledAt", nowSec);
        pending.put("count", need);
        pending.put("itemId", itemId);
        pending.put("equipType", part);
        pending.put("isNew", true);

        // === dùng EquipmentIndex helpers
        equipIdx.statsJsonOf(itemId).ifPresent(json -> pending.put("stats", json));
        equipIdx.allFieldsCanonicalOf(itemId).ifPresent(c -> pending.put("equipMeta", c));
        equipIdx.allFieldsRawStringsOf(itemId).ifPresent(r -> pending.put("equipMetaRaw", r));
        Map<Integer, Map<Integer, Map<Integer, Integer>>> pref = equipIdx.getIdxPreferred();
        Integer idxPrefId = Optional.ofNullable(pref.get(part))
                .map(m -> m.get(quality))
                .map(m -> m.get(equipLevel))
                .orElse(null);
        if (idxPrefId != null) pending.put("idxPrefId", idxPrefId);

        BoxDTOs.EquipRolled newEquip = buildEquipRolled(itemId, part);
        putRolledSnapshot(pending, newEquip);
        s.setPendingJson(writePendingJson(pending));
        CurrentEquipLookup currentLookup = findCurrentEquipWithRetry(roleId, part);
        String compareStatus = currentLookup.isLookupFailed() ? "PENDING_COMPARE_INCOMPLETE" : "PENDING_COMPARE";
        BoxDTOs.BoxCompareStateResp compareStateRolled = saveCompareState(
            roleId, newEquip, currentLookup.getEquip(), quality, equipLevel, 1, "BOX_OPEN", compareStatus);

        // Bonus reward theo colorRow (nếu có) — lọc virtual qua meta
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reward = (List<Map<String, Object>>) colorRow.get("reward");
        if (reward != null) {
            List<ItemDelta> add = new ArrayList<>();
            for (var r : reward) {
                int rid = pInt(r.get("item_id"), 0);
                long num = pLong(r.get("num"), 0L);
                if (rid > 0 && num > 0) {
                    add.add(new ItemDelta(rid, (int) num));
                    bonus.add(bonusItem(rid, num, "colorReward"));
                }
            }
            addNonVirtualItems(roleId, add, BAG_COMMON, SRC_OP_REWARD);
        }

        s.setOpenBoxTotal(s.getOpenBoxTotal() + (int) need);
        s.setLastOpenIsFive(need == 5);
        setSafeLongField(s, "lastOpenEpoch", nowSec + 1);
        boxRepo.save(s);

        rollArenaTicketIfAny(roleId, s, bonus, other);
        return BoxDTOs.OpenResp.builder()
                .pending(pending)
                .openBoxTotal(s.getOpenBoxTotal())
                .lastOpenIsFive(s.isLastOpenIsFive())
                .bonusItems(bonus)
                .isNew(1)
                .openEquip(newEquip)
                .compareState(compareStateRolled)
                .build();
    }

    /**
     * Mặc equip từ compare-state qua equip-service.
     * Sau khi mặc thành công thì luôn clear compare-state để kết thúc flow compare.
     */
    public BoxDTOs.OkResp wear(Long roleId) {
        BoxState s = getOrCreate(roleId);
        EquipDTOs.WearFromBoxItem wearItem = resolveWearItem(roleId, s);
        if (wearItem == null || wearItem.getItemId() == null || wearItem.getItemId() <= 0) {
            return BoxDTOs.OkResp.builder().ok(false).message("NO_PENDING").build();
        }
        int itemId = wearItem.getItemId();
        if (itemId <= 0) return BoxDTOs.OkResp.builder().ok(false).message("BAD_ITEM").build();

        BoxDTOs.BoxCompareStateResp beforeWearCompare = compareStateRepo.find(roleId).orElse(null);

        EquipDTOs.WearFromBoxResp out;
        try {
            out = equipFeign.wearFromBox(EquipDTOs.WearFromBoxReq.builder()
                    .roleId(String.valueOf(roleId))
                    .item(wearItem)
                    .build());
        } catch (Exception e) {
            log.warn("[box] wearFromBox failed roleId={} ex={}", roleId, e.toString());
            return BoxDTOs.OkResp.builder().ok(false).message("WEAR_FAILED").build();
        }

        EquipDTOs.ReplacedEquip replaced = out != null ? out.getReplaced() : null;
        if ((replaced == null || replaced.getItemId() == null || replaced.getItemId() <= 0) && beforeWearCompare != null) {
            replaced = replacedFromCompareState(beforeWearCompare);
            if (replaced != null && replaced.getItemId() != null && replaced.getItemId() > 0) {
                log.info("[box] wear fallback replaced from compare-state roleId={} replacedItemId={}", roleId, replaced.getItemId());
            }
        }
        int replacedItemId = replaced != null && replaced.getItemId() != null ? replaced.getItemId() : 0;
        EquipDTOs.WearFromBoxItem replacedWearItem = wearItemFromReplaced(replaced);
        boolean shouldSwapCompare = replacedItemId > 0 && !sameWearItemSnapshot(replacedWearItem, wearItem);

        if (shouldSwapCompare) {
            Map<String, Object> replacedPending = replacedWearItem.toPendingMap();
            replacedPending.put("isNew", false);
            s.setPendingJson(writePendingJson(replacedPending));
            boxRepo.save(s);

            BoxDTOs.EquipRolled replacedRolled = wearItemToEquipRolled(replacedWearItem);
            CurrentEquipLookup equippedAfterWearLookup = findCurrentEquipWithRetry(roleId, wearItem.getEquipType() != null ? wearItem.getEquipType() : 0);
            BoxDTOs.EquipRolled equippedNow = equippedAfterWearLookup.getEquip() != null
                    ? equippedAfterWearLookup.getEquip()
                    : wearItemToEquipRolled(wearItem);
            int replacedQuality = replacedWearItem.getQuality() != null ? replacedWearItem.getQuality() : 1;
            int replacedLevel = replacedWearItem.getEquipLevel() != null ? replacedWearItem.getEquipLevel() : 1;
            saveCompareState(roleId, replacedRolled, equippedNow, replacedQuality, replacedLevel, 0, "BOX_WEAR", "PENDING_COMPARE");
            log.info("[box] wear swap pending roleId={} candidateItemId={} equippedBeforeItemId={}", roleId, replacedItemId, itemId);
        } else {
            s.setPendingJson(null);
            boxRepo.save(s);
            compareStateRepo.delete(roleId);
            if (replacedItemId > 0) {
                log.warn("[box] skip swap compare because replaced equals candidate roleId={} itemId={}", roleId, itemId);
            }
        }

        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    /**
     * Bán equip (tối thiểu): clear pending. (Coin nên do wallet-service xử lý; ở đây không cộng tiền ảo)
     */
    @Transactional
    public BoxDTOs.SellResp sell(Long roleId) {
        BoxState s = getOrCreate(roleId);

        EquipDTOs.WearFromBoxItem activeItem = resolveWearItem(roleId, s);
        if (activeItem == null || activeItem.getItemId() == null || activeItem.getItemId() <= 0) {
            return BoxDTOs.SellResp.builder().ok(false).message("No pending equip").build();
        }
        Map<String, Object> activeItemMap = activeItem.toPendingMap();

        Map<String, Object> req = Map.of(
                "roleId", roleId,
            "item", activeItemMap
        );

        Map<String, Object> out;
        try {
            out = equipFeign.computeSell(req);
        } catch (Exception e) {
            log.warn("equipFeign.computeSell failed: {}", e.getMessage());
            return BoxDTOs.SellResp.builder().ok(false).message("Sell compute failed").build();
        }

        long sellCoin = pLong(out.get("coin"), 0);
        long sellExp = pLong(out.get("exp"), 0);

        if (s.isLastOpenIsFive() && Boolean.TRUE.equals(activeItem.getIsNew())) {
            if (sellCoin > 0) sellCoin *= 5;
            if (sellExp > 0) sellExp *= 5;
        }

        if (sellCoin > 0) {
            try {
                ResultDTO<WalletDTOs.MutateResp> walletResp = walletFeign.batchAdd(
                        WalletDTOs.BatchReq.builder()
                                .roleId(String.valueOf(roleId))
                                .changes(List.of(WalletDTOs.Change.builder()
                                        .itemId(1L)
                                        .amount(sellCoin)
                                        .build()))
                                .reason(SRC_MSG_BOX)
                                .reasonType(SRC_OP_REWARD)
                                .idemKey("box:sell:" + roleId + ":" + Instant.now().toEpochMilli())
                                .build()
                );
                if (walletResp == null || walletResp.getCode() != 0) {
                    String msg = walletResp == null ? "NULL" : walletResp.getMessage();
                    log.warn("walletFeign.batchAdd failed roleId={} msg={}", roleId, msg);
                }
            } catch (Exception e) {
                log.warn("walletFeign.batchAdd exception roleId={} ex={}", roleId, e.toString());
            }
        }
        if (sellExp > 0) {
            try {
                roleFeign.addExp(new RoleDTOs.AddExpReq(String.valueOf(roleId), sellExp));
            } catch (Exception e) {
                log.warn("roleFeign.addExp failed: {}", e.getMessage());
            }
        }

        s.setLastOpenIsFive(false);
    s.setPendingJson(null);
        boxRepo.save(s);
        compareStateRepo.delete(roleId);
        return BoxDTOs.SellResp.builder()
                .ok(true)
                .message("OK")
                .sellCoin(sellCoin)
                .sellExp(sellExp)
                .build();
    }

    /**
     * Mua lần mở (demo): tăng boxBuyTimes.
     */
    @Transactional
    public BoxDTOs.OkResp buy(Long roleId) {
        BoxState s = getOrCreate(roleId);
        setSafeIntField(s, "boxBuyTimes", s.getBoxBuyTimes() + 1);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    /**
     * Nâng cấp hộp (demo): set timer 60s.
     */
    @Transactional
    public BoxDTOs.OkResp levelUp(Long roleId) {
        BoxState s = getOrCreate(roleId);
        long now = Instant.now().getEpochSecond();
        s.setLevelUpEndEpoch(now + 60);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("UPGRADING").build();
    }

    /**
     * Nhận thưởng level (demo): bật cờ.
     */
    @Transactional
    public BoxDTOs.OkResp levelReward(Long roleId, int idx) {
        BoxState s = getOrCreate(roleId);
        s.setLevelFetchFlag(s.getLevelFetchFlag() | (1 << Math.max(0, Math.min(30, idx))));
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    // ========= LUCK (tối thiểu) =========

    public BoxDTOs.LuckInfoResp luckInfo(Long roleId) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> snapshotLuckOpen(roleId, luckEventDays()));
        BoxState bs = getOrCreate(roleId);
        return BoxDTOs.LuckInfoResp.builder()
                .endTimestamp(ls.getEndEpoch())
                .receiveFlag(ls.getReceiveBitmap())
                .openBoxNumDelta(Math.max(0, bs.getOpenBoxTotal() - ls.getSnapshotOpenCnt()))
                .boxLevel(bs.getBoxLevel())
                .build();
    }

    @Transactional
    public BoxDTOs.OkResp luckReceive(Long roleId, int seq) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> snapshotLuckOpen(roleId, luckEventDays()));
        long now = Instant.now().getEpochSecond();
        if (ls.getEndEpoch() > 0 && ls.getEndEpoch() <= now)
            return BoxDTOs.OkResp.builder().ok(false).message("LUCK_ENDED").build();

        int safeSeq = Math.max(0, Math.min(61, seq));
        long bit = 1L << safeSeq;
        if ((ls.getReceiveBitmap() & bit) != 0) {
            return BoxDTOs.OkResp.builder().ok(false).message("ALREADY_RECEIVED").build();
        }

        Optional<Map<String, Object>> rewardRowOpt = luckRewardBySeq(safeSeq);
        if (rewardRowOpt.isEmpty()) {
            return BoxDTOs.OkResp.builder().ok(false).message("BAD_SEQ").build();
        }
        Map<String, Object> rewardRow = rewardRowOpt.get();

        BoxState bs = getOrCreate(roleId);
        int typeBoxNum = pInt(rewardRow.get("type_box_num"), 0);
        int need = pInt(rewardRow.get("type_num"), Integer.MAX_VALUE);
        int progress = (typeBoxNum == 2)
                ? bs.getBoxLevel()
                : Math.max(0, bs.getOpenBoxTotal() - ls.getSnapshotOpenCnt());
        if (progress < need) {
            return BoxDTOs.OkResp.builder().ok(false).message(MSG_NOT_REACHED).build();
        }

        Map<String, Object> rewardItem = asObjMap(rewardRow.get("reward_item"));
        int rewardItemId = pInt(rewardItem.get("item_id"), 0);
        int rewardNum = pInt(rewardItem.get("num"), 0);
        if (rewardItemId > 0 && rewardNum > 0) {
            addNonVirtualItems(roleId, List.of(new ItemDelta(rewardItemId, rewardNum)), BAG_COMMON, SRC_OP_REWARD);
        }

        ls.setReceiveBitmap(ls.getReceiveBitmap() | bit);
        luckRepo.save(ls);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    @Transactional
    public BoxDTOs.OkResp quicken(Long roleId, int num) {
        num = Math.max(1, num);
        BoxState s = getOrCreate(roleId);
        maybeCompleteLevelUp(s);

        long now = Instant.now().getEpochSecond();
        long left = s.getLevelUpEndEpoch() - now;
        if (left <= 0) return BoxDTOs.OkResp.builder().ok(false).message("No level up").build();

        var other = cfgOther();
        int quickId = pInt(other.get("accelerate_id"), 0);
        int secPerItem = pInt(other.get("accelerate_sec"), 60);

        if (quickId > 0) {
            BagConsumeReq consumeReq = BagConsumeReq.builder()
                    .userId(1L) // audit field
                    .roleId(roleId)
                    .itemId(quickId)
                    .amount(num)
                    .source("BOX_QUICKEN")
                    .build();
            bag.consume(consumeReq);
        }

        long reduce = (long) num * Math.max(1, secPerItem);
        long newEnd = s.getLevelUpEndEpoch() - reduce;

        if (newEnd <= now) {
            s.setLevelUpEndEpoch(now);
            boxRepo.save(s);
            maybeCompleteLevelUp(s);
            return BoxDTOs.OkResp.builder().ok(true).message("Quickened to completion").build();
        } else {
            s.setLevelUpEndEpoch(newEnd);
            boxRepo.save(s);
            return BoxDTOs.OkResp.builder().ok(true).message("Quickened").build();
        }
    }

    private Map<String, Object> cfgOther() {
        // unpack.other() nhiều nơi là list<map>; convert về key->value cho tiện.
        // Chấp nhận key đụng nhau thì lấy phần tử đầu.
        var list = unpackCfg.other();
        Map<String, Object> m = new LinkedHashMap<>();
        if (list != null) {
            for (Map<String, String> r : list) {
                for (var e : r.entrySet()) {
                    m.putIfAbsent(e.getKey(), e.getValue());
                }
            }
        }
        return m;
    }

    // ===== Setting =====
    public BoxDTOs.BoxSettingResp getSetting(Long roleId) {
        var s = getOrCreateSetting(roleId);
        return mapSetting(s);
    }

    public BoxDTOs.BoxSettingResp saveSetting(Long roleId, BoxDTOs.BoxSettingResp req) {
        var s = getOrCreateSetting(roleId);
        s.setEquipEqality(req.getEquipEqality());
        s.setOpenFiveMark(req.getOpenFiveMark());
        s.setEquipCapMark(req.getEquipCapMark());
        s.setEquipSellMark(req.getEquipSellMark());
        s.setConditionFirst1(req.getConditionFirst1());
        s.setConditionFirst2(req.getConditionFirst2());
        s.setConditionSecond1(req.getConditionSecond1());
        s.setConditionSecond2(req.getConditionSecond2());
        s.setConditionFirstMark(req.getConditionFirstMark());
        s.setConditionSecondMark(req.getConditionSecondMark());
        s.setRetainMark(req.getRetainMark());
        s.setChallengeMark(req.getChallengeMark());
        settingRepo.save(s);
        return mapSetting(s);
    }

    public BoxDTOs.BoxCompareStateResp getCompareState(Long roleId) {
        return compareStateRepo.find(roleId).orElse(null);
    }

    public void clearCompareState(Long roleId) {
        compareStateRepo.delete(roleId);
    }

    private EquipDTOs.WearFromBoxItem resolveWearItem(Long roleId, BoxState state) {
        Map<String, Object> pending = parsePendingJsonSafe(state != null ? state.getPendingJson() : null);
        EquipDTOs.WearFromBoxItem fromPending = EquipDTOs.WearFromBoxItem.fromPending(pending);
        if (fromPending != null && fromPending.getItemId() != null && fromPending.getItemId() > 0) {
            return fromPending;
        }

        BoxDTOs.BoxCompareStateResp compareState = compareStateRepo.find(roleId).orElse(null);
        EquipDTOs.WearFromBoxItem fromCompareState = wearItemFromCompareState(compareState);
        if (fromCompareState != null && fromCompareState.getItemId() != null && fromCompareState.getItemId() > 0) {
            if (state != null) {
                state.setPendingJson(writePendingJson(fromCompareState.toPendingMap()));
                boxRepo.save(state);
            }
            return fromCompareState;
        }
        return null;
    }

    private EquipDTOs.WearFromBoxItem wearItemFromCompareState(BoxDTOs.BoxCompareStateResp compareState) {
        if (compareState == null || compareState.getCandidateEquip() == null) {
            return null;
        }
        BoxDTOs.BoxCompareSnapshotDTO candidate = compareState.getCandidateEquip();
        return EquipDTOs.WearFromBoxItem.builder()
                .kind("equip")
                .itemId(candidate.getItemId())
                .equipType(candidate.getEquipType())
                .quality(candidate.getQuality())
                .equipLevel(candidate.getEquipLevel())
                .hp(candidate.getHp())
                .attack(candidate.getAttack())
                .defend(candidate.getDefend())
                .speed(candidate.getSpeed())
                .attrType1(candidate.getAttrType1())
                .attrValue1(candidate.getAttrValue1())
                .attrType2(candidate.getAttrType2())
                .attrValue2(candidate.getAttrValue2())
                .isNew(compareState.getIsNew() != null ? compareState.getIsNew() != 0 : null)
                .build();
    }

    private Map<String, Object> toPendingMap(BoxDTOs.BoxCompareStateResp compareState) {
        EquipDTOs.WearFromBoxItem wearItem = wearItemFromCompareState(compareState);
        return wearItem != null ? wearItem.toPendingMap() : null;
    }

    private EquipDTOs.WearFromBoxItem wearItemFromReplaced(EquipDTOs.ReplacedEquip replaced) {
        if (replaced == null || replaced.getItemId() == null || replaced.getItemId() <= 0) {
            return null;
        }
        return EquipDTOs.WearFromBoxItem.builder()
                .kind("equip")
                .itemId(replaced.getItemId())
                .equipType(replaced.getEquipType())
                .quality(replaced.getQuality())
                .equipLevel(replaced.getEquipLevel())
                .hp(replaced.getHp())
                .attack(replaced.getAttack())
                .defend(replaced.getDefend())
                .speed(replaced.getSpeed())
                .attrType1(replaced.getAttrType1())
                .attrValue1(replaced.getAttrValue1())
                .attrType2(replaced.getAttrType2())
                .attrValue2(replaced.getAttrValue2())
                .isNew(false)
                .build();
    }

    private EquipDTOs.ReplacedEquip replacedFromCompareState(BoxDTOs.BoxCompareStateResp compareState) {
        if (compareState == null || compareState.getEquippedBefore() == null) {
            return null;
        }
        BoxDTOs.BoxCompareSnapshotDTO before = compareState.getEquippedBefore();
        if (before.getItemId() == null || before.getItemId() <= 0) {
            return null;
        }
        return EquipDTOs.ReplacedEquip.builder()
                .itemId(before.getItemId())
                .equipType(before.getEquipType())
                .quality(before.getQuality())
                .equipLevel(before.getEquipLevel())
                .hp(before.getHp())
                .attack(before.getAttack())
                .defend(before.getDefend())
                .speed(before.getSpeed())
                .attrType1(before.getAttrType1())
                .attrValue1(before.getAttrValue1())
                .attrType2(before.getAttrType2())
                .attrValue2(before.getAttrValue2())
                .build();
    }

    private BoxDTOs.EquipRolled wearItemToEquipRolled(EquipDTOs.WearFromBoxItem item) {
        if (item == null) {
            return null;
        }
        return BoxDTOs.EquipRolled.builder()
                .equipType(item.getEquipType())
                .itemId(item.getItemId())
                .hp(item.getHp())
                .attack(item.getAttack())
                .defend(item.getDefend())
                .speed(item.getSpeed())
                .attrType1(item.getAttrType1())
                .attrValue1(item.getAttrValue1())
                .attrType2(item.getAttrType2())
                .attrValue2(item.getAttrValue2())
                .build();
    }

    private boolean sameWearItemSnapshot(EquipDTOs.WearFromBoxItem left, EquipDTOs.WearFromBoxItem right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getItemId(), right.getItemId())
                && Objects.equals(left.getEquipType(), right.getEquipType())
                && Objects.equals(left.getQuality(), right.getQuality())
                && Objects.equals(left.getEquipLevel(), right.getEquipLevel())
                && Objects.equals(left.getHp(), right.getHp())
                && Objects.equals(left.getAttack(), right.getAttack())
                && Objects.equals(left.getDefend(), right.getDefend())
                && Objects.equals(left.getSpeed(), right.getSpeed())
                && Objects.equals(left.getAttrType1(), right.getAttrType1())
                && Objects.equals(left.getAttrValue1(), right.getAttrValue1())
                && Objects.equals(left.getAttrType2(), right.getAttrType2())
                && Objects.equals(left.getAttrValue2(), right.getAttrValue2());
    }

    private Map<String, Object> parsePendingJsonSafe(String pendingJson) {
        if (!StringUtils.hasText(pendingJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(pendingJson, MAP_TYPE);
        } catch (Exception e) {
            log.warn("[box] parse pendingJson failed ex={}", e.toString());
            return null;
        }
    }

    private String writePendingJson(Map<String, Object> pending) {
        if (pending == null || pending.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pending);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize pendingJson", e);
        }
    }

    private BoxSetting getOrCreateSetting(Long roleId) {
        settingRepo.insertDefaultIfAbsent(roleId);
        return settingRepo.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("BoxSetting not found for roleId=" + roleId));
    }

    private BoxDTOs.BoxSettingResp mapSetting(BoxSetting s) {
        return BoxDTOs.BoxSettingResp.builder()
                .equipEqality(s.getEquipEqality())
                .openFiveMark(s.getOpenFiveMark())
                .equipCapMark(s.getEquipCapMark())
                .equipSellMark(s.getEquipSellMark())
                .conditionFirst1(s.getConditionFirst1())
                .conditionFirst2(s.getConditionFirst2())
                .conditionSecond1(s.getConditionSecond1())
                .conditionSecond2(s.getConditionSecond2())
                .conditionFirstMark(s.getConditionFirstMark())
                .conditionSecondMark(s.getConditionSecondMark())
                .retainMark(s.getRetainMark())
                .challengeMark(s.getChallengeMark())
                .build();
    }


    /**
     * DECOMPOSE: nhờ equip-service trả vật liệu + exp; x5 nếu phù hợp; clear pending & reset cờ.
     */
    @Transactional
    public BoxDTOs.DecomposeResp decompose(Long roleId) {
        BoxState s = getOrCreate(roleId);

        EquipDTOs.WearFromBoxItem activeItem = resolveWearItem(roleId, s);
        if (activeItem == null || activeItem.getItemId() == null || activeItem.getItemId() <= 0) {
            return BoxDTOs.DecomposeResp.builder().ok(false).message("No pending equip").build();
        }
        Map<String, Object> activeItemMap = activeItem.toPendingMap();

        Map<String, Object> req = Map.of(
                "roleId", roleId,
            "item", activeItemMap
        );
        Map<String, Object> out;
        try {
            out = equipFeign.decompose(req); // trả { itemId, num, exp } – đổi tên nếu API bạn khác
        } catch (Exception e) {
            log.warn("equipFeign.decompose failed: {}", e.getMessage());
            return BoxDTOs.DecomposeResp.builder().ok(false).message("Decompose compute failed").build();
        }

        int gotItemId = pInt(out.get("itemId"), 0);
        long gotNum = pLong(out.get("num"), 0);
        long gotExp = pLong(out.get("exp"), 0);

        if (s.isLastOpenIsFive() && Boolean.TRUE.equals(activeItem.getIsNew())) {
            if (gotNum > 0) gotNum *= 5;
            if (gotExp > 0) gotExp *= 5;
        }

        if (gotItemId > 0 && gotNum > 0) {
            Map<String, Object> gotItemMeta = loadItemMeta(gotItemId);
            BagAddItemReq addReq = BagAddItemReq.builder()
                    .userId(1L) // audit field
                    .roleId(roleId)
                .items(List.of(buildBagAddItem(gotItemId, (int) gotNum, BAG_COMMON, gotItemMeta)))
                    .source("BOX_DECOMPOSE_ITEM")
                    .build();
            bag.add(addReq);
        }
        if (gotExp > 0) {
            try {
                roleFeign.addExp(new RoleDTOs.AddExpReq(String.valueOf(roleId), gotExp));
            } catch (Exception e) {
                log.warn("roleFeign.addExp failed: {}", e.getMessage());
            }
        }

        s.setLastOpenIsFive(false);
    s.setPendingJson(null);
        boxRepo.save(s);
        compareStateRepo.delete(roleId);

        return BoxDTOs.DecomposeResp.builder()
                .ok(true)
                .gotItemId(gotItemId)
                .gotNum(gotNum)
                .gotExp(gotExp)
                .message("Decomposed")
                .build();
    }

    // ========= INTERNAL HELPERS =========

    private BoxState getOrCreate(Long roleId) {
        boxRepo.insertDefaultIfAbsent(roleId);
        return boxRepo.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("BoxState not found for roleId=" + roleId));
    }

    private void maybeCompleteLevelUp(BoxState s) {
        long now = Instant.now().getEpochSecond();
        if (s.getLevelUpEndEpoch() > 0 && s.getLevelUpEndEpoch() <= now) {
            s.setBoxLevel(Math.max(1, s.getBoxLevel() + 1));
            s.setLevelUpEndEpoch(0);
            boxRepo.save(s);
        }
    }

    private void maybeDailyReset(BoxState s) {
        String todayUtc = LocalDate.now(ZoneOffset.UTC).toString();
        if (!todayUtc.equals(s.getDailyYmd())) {
            setSafeIntField(s, "shiZhuangNum", 0);
            setSafeIntField(s, "arenaItemNum", 0);
            s.setDailyYmd(todayUtc);
            boxRepo.save(s);
        }
    }

    /**
     * Trả “bước” cần đi để tới fixed reward tiếp theo. Chưa có data -> 0.
     */
    private int nextFixedStep(int opened, boolean isFive) {
        var rows = unpackCfg.fixedReward(); // [{box_oder,item_id},...]
        if (rows == null || rows.isEmpty()) return 0;
        int nextOrder = Integer.MAX_VALUE;
        for (var r : rows) {
            int od = pInt(r.get("box_oder"), 0);
            if (od > opened && od < nextOrder) nextOrder = od;
        }
        if (nextOrder == Integer.MAX_VALUE) return 0;
        int step = nextOrder - opened;
        // Nếu mở 5 mà bước quá xa, có thể bỏ qua (tuỳ gameplay). Ở đây vẫn trả step để “truy đuổi” fixed.
        return step;
    }

    /**
     * Fixed reward itemId cho thứ tự mở (order). Chưa có data → empty.
     */
    private Optional<Integer> fixedItemForOrder(int order) {
        var rows = unpackCfg.fixedReward();
        if (rows == null) return Optional.empty();
        for (var r : rows)
            if (pInt(r.get("box_oder"), 0) == order)
                return Optional.of(pInt(r.get("item_id"), 0));
        return Optional.empty();
    }

    /**
     * Thời trang: demo pick theo tỉ lệ nếu bạn có `shizhuangRate()`; ở đây trả empty cho an toàn.
     */
    private Optional<Integer> pickFashionId() {
        var rs = unpackCfg.shizhuangRate(); // [{seq,item_id,rate}]
        if (rs == null || rs.isEmpty()) return Optional.empty();
        int sum = 0;
        List<int[]> t = new ArrayList<>();
        for (var r : rs) {
            int id = pInt(r.get("item_id"), 0);
            int rate = pInt(r.get("rate"), 0);
            if (id > 0 && rate > 0) {
                t.add(new int[]{id, rate});
                sum += rate;
            }
        }
        if (sum <= 0) return Optional.empty();
        int roll = ThreadLocalRandom.current().nextInt(sum), acc = 0;
        for (var e : t) {
            acc += e[1];
            if (roll < acc) return Optional.of(e[0]);
        }
        return Optional.empty();
    }

    private Map<String, Object> colorRowByLevel(int boxLevel) {
        var rows = unpackCfg.randomColor();
        if (rows == null || rows.isEmpty()) return Map.of();

        List<Map<String, Object>> matched = new ArrayList<>();
        for (var r : rows) {
            if (pInt(r.get("box_level"), -1) == boxLevel) {
                matched.add(r);
            }
        }
        List<Map<String, Object>> pool = matched.isEmpty() ? rows : matched;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /**
     * Lấy quality theo trọng số trong colorRow hoặc fallback 1..8.
     */
    private int rollQuality(Map<String, Object> colorRow, boolean isFive) {
        // Nếu colorRow có "rate1..8" → rút thăm theo rate. Nếu không → uniform 1..8
        int[] rate = new int[8];
        int sum = 0;
        for (int i = 1; i <= 8; i++) {
            int r = pInt(colorRow.getOrDefault("rate" + i, 0), 0);
            rate[i - 1] = r;
            sum += r;
        }
        if (sum <= 0) {
            return 1 + ThreadLocalRandom.current().nextInt(8);
        }
        int roll = ThreadLocalRandom.current().nextInt(sum), acc = 0;
        for (int i = 0; i < 8; i++) {
            acc += rate[i];
            if (roll < acc) return i + 1;
        }
        return 1;
    }

    /**
     * Lấy level equip theo level người chơi (đơn giản: = playerLevel).
     */
    private int rollEquipLevelByPlayerLevel(int playerLevel) {
        var rows = unpackCfg.randomLevel(); // [{level,random_level,rate},...]
        if (rows == null || rows.isEmpty()) return Math.max(1, playerLevel);
        int lvl = Math.max(1, playerLevel);
        int sum = 0;
        List<int[]> table = new ArrayList<>(); // [randLevel, weight]
        for (var r : rows) {
            if (pInt(r.get("level"), 0) != lvl) continue;
            int rl = pInt(r.get("random_level"), 0);
            int rate = pInt(r.get("rate"), 0);
            if (rl > 0 && rate > 0) {
                table.add(new int[]{rl, rate});
                sum += rate;
            }
        }
        if (sum <= 0) return lvl;
        int roll = ThreadLocalRandom.current().nextInt(sum), acc = 0;
        for (var e : table) {
            acc += e[1];
            if (roll < acc) return e[0];
        }
        return lvl;
    }

    /**
     * Resolve itemId từ (equipType, quality, equipLevel). Nếu chưa có bảng map → fallback MINIMAL_EQUIP_ITEM_ID.
     */
    private int resolveItemId(int part, int quality, int equipLevel) {
        return equipIdx.resolve(part, quality, equipLevel)
                .orElseThrow(() -> new IllegalStateException(
                        "No equipment itemId for part=" + part + " q=" + quality + " lv=" + equipLevel));
    }

    private int fetchPlayerLevel(Long roleId, int hintedLevel) {
        int fallback = Math.max(1, hintedLevel);
        try {
            RoleDTOs.RoleResp role = roleFeign.detail(String.valueOf(roleId));
            if (role != null && role.getLevel() != null && role.getLevel() > 0) {
                return role.getLevel();
            }
        } catch (Exception e) {
            log.debug("[box] role-service level fetch failed roleId={}: {}", roleId, e.toString());
        }
        return fallback;
    }

    // ========= EQUIP ROLL & COMPARE HELPERS =========

    /** Roll một giá trị đơn từ [min, max]. Trả null nếu cả hai đều = 0 (không có chỉ số này). */
    private Integer rollRangeOrNull(long min, long max) {
        if (min == 0 && max == 0) return null;
        if (max <= min) return (int) min;
        return (int) ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * Xây EquipRolled có giá trị đơn (rolled) từ config EquipmentIndex.
     * Dùng cho SC_BOX_EQUIP_INFO popup và so sánh cũ/mới.
     */
    private BoxDTOs.EquipRolled buildEquipRolled(int itemId, int part) {
        return equipIdx.rowOf(itemId).map(r -> BoxDTOs.EquipRolled.builder()
                .equipType(part)
                .itemId(itemId)
                .hp(rollRangeOrNull(r.getHpMin(), r.getHpMax()))
                .attack(rollRangeOrNull(r.getAttMin(), r.getAttMax()))
                .defend(rollRangeOrNull(r.getDefMin(), r.getDefMax()))
                .speed(rollRangeOrNull(r.getSpeedMin(), r.getSpeedMax()))
                .attrType1((int) r.getFristAtt())
                .attrValue1(null)             // secondary attr value computed elsewhere
                .attrType2((int) r.getSecondAtt())
                .attrValue2(null)
                .build()).orElse(null);
    }

    private void putRolledSnapshot(Map<String, Object> pending, BoxDTOs.EquipRolled rolled) {
        if (pending == null || rolled == null) return;
        if (rolled.getItemId() != null) pending.put("itemId", rolled.getItemId());
        if (rolled.getEquipType() != null) pending.put("equipType", rolled.getEquipType());
        if (rolled.getHp() != null) pending.put("hp", rolled.getHp());
        if (rolled.getAttack() != null) pending.put("attack", rolled.getAttack());
        if (rolled.getDefend() != null) pending.put("defend", rolled.getDefend());
        if (rolled.getSpeed() != null) pending.put("speed", rolled.getSpeed());
        if (rolled.getAttrType1() != null) pending.put("attr_type1", rolled.getAttrType1());
        if (rolled.getAttrValue1() != null) pending.put("attr_value1", rolled.getAttrValue1());
        if (rolled.getAttrType2() != null) pending.put("attr_type2", rolled.getAttrType2());
        if (rolled.getAttrValue2() != null) pending.put("attr_value2", rolled.getAttrValue2());
    }

    /**
     * Tìm equip hiện tại trong slot (part) của player từ BAG_EQUIP để so sánh cũ vs mới.
     * Trả null nếu chưa có equip ở slot đó hoặc không thể lấy từ bag-service.
     */
    private CurrentEquipLookup findCurrentEquipOnce(Long roleId, int part) {
        try {
            EquipDTOs.ListResp resp = equipFeign.list(String.valueOf(roleId));
            if (resp == null || resp.getItems() == null) {
                return CurrentEquipLookup.failed(null);
            }
            for (EquipDTOs.EquipItem item : resp.getItems()) {
                if (item.getEquipType() == part && item.getItemId() > 0) {
                    return CurrentEquipLookup.success(BoxDTOs.EquipRolled.builder()
                            .equipType(item.getEquipType())
                            .itemId(item.getItemId())
                            .hp(item.getHp())
                            .attack(item.getAttack())
                            .defend(item.getDefend())
                            .speed(item.getSpeed())
                            .attrType1(item.getAttrType1())
                            .attrValue1(item.getAttrValue1())
                            .attrType2(item.getAttrType2())
                            .attrValue2(item.getAttrValue2())
                            .build());
                }
            }
            return CurrentEquipLookup.success(null);
        } catch (Exception e) {
            log.debug("[box] list current-equip miss roleId={} part={} ex={}", roleId, part, e.toString());
        }

        try {
            EquipDTOs.EquipItem snapshot = equipFeign.snapshot(roleId, part);
            if (snapshot != null && snapshot.getItemId() > 0) {
                return CurrentEquipLookup.success(BoxDTOs.EquipRolled.builder()
                        .equipType(snapshot.getEquipType())
                        .itemId(snapshot.getItemId())
                        .hp(snapshot.getHp())
                        .attack(snapshot.getAttack())
                        .defend(snapshot.getDefend())
                        .speed(snapshot.getSpeed())
                        .attrType1(snapshot.getAttrType1())
                        .attrValue1(snapshot.getAttrValue1())
                        .attrType2(snapshot.getAttrType2())
                        .attrValue2(snapshot.getAttrValue2())
                        .build());
            }
        } catch (Exception e) {
            log.debug("[box] snapshot current-equip miss roleId={} part={} ex={}", roleId, part, e.toString());
        }

        return CurrentEquipLookup.failed(null);
    }

    private CurrentEquipLookup findCurrentEquipWithRetry(Long roleId, int part) {
        CurrentEquipLookup first = findCurrentEquipOnce(roleId, part);
        if (!first.isLookupFailed()) {
            return first;
        }

        // Retry once to reduce transient misses from equip-service before opening compare popup.
        try {
            Thread.sleep(80L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return first;
        }
        CurrentEquipLookup second = findCurrentEquipOnce(roleId, part);
        return second.isLookupFailed() ? first : second;
    }

    private BoxDTOs.BoxCompareStateResp saveCompareState(Long roleId,
                                                         BoxDTOs.EquipRolled candidateEquip,
                                                         BoxDTOs.EquipRolled equippedBefore,
                                                         Integer quality,
                                                         Integer equipLevel,
                                                         Integer isNew,
                                                         String source,
                                                         String status) {
        if (roleId == null || candidateEquip == null) {
            return null;
        }

        BoxDTOs.BoxCompareSnapshotDTO candidate = BoxDTOs.BoxCompareSnapshotDTO.fromEquipRolled(candidateEquip);
        if (candidate != null) {
            candidate.setQuality(quality);
            candidate.setEquipLevel(equipLevel);
        }

        BoxDTOs.BoxCompareSnapshotDTO current = BoxDTOs.BoxCompareSnapshotDTO.fromEquipRolled(equippedBefore);
        BoxDTOs.BoxCompareStateResp state = BoxDTOs.BoxCompareStateResp.builder()
                .roleId(roleId)
                .stateVersion(UUID.randomUUID().toString())
                .source(source)
                .status((status != null && !status.isBlank()) ? status : "PENDING_COMPARE")
                .openedAt(Instant.now().getEpochSecond())
                .isNew(isNew)
                .candidateEquip(candidate)
                .equippedBefore(current)
                .build();
        compareStateRepo.save(state);
        Integer candidateItemId = candidate != null ? candidate.getItemId() : null;
        Integer equippedBeforeItemId = current != null ? current.getItemId() : null;
        String candidateStats = candidate == null
                ? "null"
                : String.format("hp=%s atk=%s def=%s spd=%s", candidate.getHp(), candidate.getAttack(), candidate.getDefend(), candidate.getSpeed());
        String equippedBeforeStats = current == null
                ? "null"
                : String.format("hp=%s atk=%s def=%s spd=%s", current.getHp(), current.getAttack(), current.getDefend(), current.getSpeed());
        log.info("[box.compare.state] roleId={} stateVersion={} source={} status={} candidateItemId={} equippedBeforeItemId={} candidateStats={} equippedBeforeStats={}",
            roleId,
            state.getStateVersion(),
            source,
            state.getStatus(),
            candidateItemId,
            equippedBeforeItemId,
            candidateStats,
            equippedBeforeStats);
        return state;
    }

    private static final class CurrentEquipLookup {
        private final BoxDTOs.EquipRolled equip;
        private final boolean lookupFailed;

        private CurrentEquipLookup(BoxDTOs.EquipRolled equip, boolean lookupFailed) {
            this.equip = equip;
            this.lookupFailed = lookupFailed;
        }

        static CurrentEquipLookup success(BoxDTOs.EquipRolled equip) {
            return new CurrentEquipLookup(equip, false);
        }

        static CurrentEquipLookup failed(BoxDTOs.EquipRolled equip) {
            return new CurrentEquipLookup(equip, true);
        }

        BoxDTOs.EquipRolled getEquip() {
            return equip;
        }

        boolean isLookupFailed() {
            return lookupFailed;
        }
    }

    private void rollArenaTicketIfAny(Long roleId, BoxState s, List<Map<String, Object>> bonus, Map<String, String> other) {
        if (arenaTicketItemId <= 0) return; // disabled by config
        // Add arena ticket to the response bonus list
        bonus.add(bonusItem(arenaTicketItemId, 1, "arenaTicket"));
        // Grant actual item to bag
        addNonVirtualItems(roleId,
                List.of(ItemDelta.builder().itemId(arenaTicketItemId).amount(1).build()),
                BAG_COMMON, SRC_OP_REWARD);
        log.info("[box] Arena ticket itemId={} granted to roleId={}", arenaTicketItemId, roleId);
    }

    // ========= BAG helpers =========

    /** Chỉ add vật phẩm **không-ảo** vào bag (tránh lỗi VIRTUAL_ITEM_USE_WALLET). */
    /**
     * Lọc virtual bằng ItemFeign + knowledge từ equipment.json.
     */
    private void addNonVirtualItems(Long roleId, List<BagDTOs.ItemDelta> items, byte bagType, int srcOp) {
        if (items == null || items.isEmpty()) return;

        // Nhanh: tách item có thể chắc chắn NON-VIRTUAL (equip)
        List<BagDTOs.ItemDelta> equipItems = new ArrayList<>();
        List<BagDTOs.ItemDelta> unknown = new ArrayList<>();
        for (var it : items) {
            int id = it.getItemId();
            if (equipIdx.isEquipId(id)) equipItems.add(it);
            else unknown.add(it);
        }

        List<BagDTOs.ItemDelta> finalAdd = new ArrayList<>(equipItems);

        // Tra meta cho phần unknown để loại virtual
        if (!unknown.isEmpty()) {
            String csv = unknown.stream().map(i -> String.valueOf(i.getItemId())).distinct().reduce((a, b) -> a + "," + b).orElse("");
            try {
                Map<Integer, Map<String, Object>> metas = batchItemMeta(csv);
                for (var it : unknown) {
                    var m = metas.get(it.getItemId());
                    int isVirtual = m == null ? 0 : pInt(m.get("isVirtual"), 0);
                    if (isVirtual == 0) finalAdd.add(it);
                }
            } catch (Throwable t) {
                log.warn("[box] itemFeign.meta error: {}", t.toString());
                // Thận trọng: nếu không tra được, KHÔNG add để tránh VIRTUAL_ITEM_USE_WALLET
            }
        }

        if (!finalAdd.isEmpty()) {
            try {
                String csv = finalAdd.stream()
                    .map(d -> String.valueOf(d.getItemId()))
                    .distinct()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
                Map<Integer, Map<String, Object>> metaLookup;
                try {
                    metaLookup = batchItemMeta(csv);
                } catch (Throwable t) {
                    log.warn("[box] itemFeign.meta add fallback rid={} ex={}", roleId, t.toString());
                    metaLookup = Map.of();
                }
                final Map<Integer, Map<String, Object>> finalMetas = metaLookup;
                List<BagAddItemReq.Item> bagItems = finalAdd.stream()
                    .map(d -> buildBagAddItem(
                        d.getItemId(),
                        d.getAmount(),
                        bagType,
                        finalMetas.get(d.getItemId())))
                        .toList();
                BagAddItemReq addReq = BagAddItemReq.builder()
                        .userId(1L) // audit field
                        .roleId(roleId)
                        .items(bagItems)
                        .source("BOX_REWARD")
                        .build();
                bag.add(addReq);
            } catch (Throwable t) {
                log.warn("[box] bag.add error rid={} ex={}", roleId, t.toString());
            }
        }
    }

    private Map<String, Object> loadItemMeta(int itemId) {
        if (itemId <= 0) return Map.of();
        try {
            return batchItemMeta(String.valueOf(itemId)).getOrDefault(itemId, Map.of());
        } catch (Throwable t) {
            log.warn("[box] itemFeign.meta single error itemId={} ex={}", itemId, t.toString());
            return Map.of();
        }
    }

    private Map<Integer, Map<String, Object>> batchItemMeta(String csv) {
        if (!StringUtils.hasText(csv)) return Map.of();

        List<Integer> itemIds = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(id -> {
                    try {
                        return Integer.valueOf(id);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) return Map.of();

        Map<Integer, Map<String, Object>> metas = itemFeign.batchMeta(itemIds);
        return metas == null ? Map.of() : metas;
    }

    private BagAddItemReq.Item buildBagAddItem(int itemId, int amount, int bagType, Map<String, Object> meta) {
        return BagAddItemReq.Item.builder()
                .itemId(itemId)
                .amount(amount)
                .quality(resolveBagItemQuality(itemId, meta))
                .bagType(bagType)
                .build();
    }

    private int resolveBagItemQuality(int itemId, Map<String, Object> meta) {
        if (equipIdx.isEquipId(itemId)) {
            return equipIdx.findPQLById(itemId)
                    .map(pql -> Math.max(1, pql[1]))
                    .orElseGet(() -> Math.max(1, pInt(meta == null ? null : firstNonNull(meta.get("quality"), meta.get("color"), meta.get("q")), 1)));
        }
        return Math.max(1, pInt(meta == null ? null : firstNonNull(meta.get("quality"), meta.get("color"), meta.get("q")), 1));
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }


    // ========= UTILS =========

    private int pInt(Object v, int def) {
        try {
            if (v == null) return def;
            if (v instanceof Number n) return n.intValue();
            String s = String.valueOf(v);
            if (s.isBlank()) return def;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private long pLong(Object v, long def) {
        try {
            if (v == null) return def;
            if (v instanceof Number n) return n.longValue();
            String s = String.valueOf(v);
            if (s.isBlank()) return def;
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    private int getSafeIntField(BoxState s, String name) {
        try {
            var m = BoxState.class.getMethod("get" + cap(name));
            Object v = m.invoke(s);
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private void setSafeIntField(BoxState s, String name, int val) {
        try {
            var m = BoxState.class.getMethod("set" + cap(name), int.class);
            m.invoke(s, val);
        } catch (Throwable ignore) {
        }
    }

    private long getSafeLongField(BoxState s, String name) {
        try {
            var m = BoxState.class.getMethod("get" + cap(name));
            Object v = m.invoke(s);
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Throwable ignore) {
            return 0L;
        }
    }

    private void setSafeLongField(BoxState s, String name, long val) {
        try {
            var m = BoxState.class.getMethod("set" + cap(name), long.class);
            m.invoke(s, val);
        } catch (Throwable ignore) {
        }
    }

    private void trySetIntField(BoxState s, String name, int val) {
        try {
            setSafeIntField(s, name, val);
        } catch (Throwable ignore) {
        }
    }

    private void trySetLongField(BoxState s, String name, long val) {
        try {
            setSafeLongField(s, name, val);
        } catch (Throwable ignore) {
        }
    }

    private String cap(String s) {
        if (!StringUtils.hasText(s)) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Map<String, Object> bonusItem(long itemId, long num, String tag) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", itemId);
        m.put("num", num);
        m.put("tag", tag);
        return m;
    }

    // ========= Luck bootstrap (demo) =========
    private LuckState snapshotLuckOpen(Long roleId, int days) {
        long now = Instant.now().getEpochSecond();
        LuckState ls = new LuckState();
        ls.setRoleId(roleId);
        if (days > 0) {
            ls.setStartEpoch(now);
            ls.setEndEpoch(now + days * 86400L);
        } else {
            ls.setStartEpoch(0L);
            ls.setEndEpoch(0L);
        }
        BoxState bs = getOrCreate(roleId);
        ls.setSnapshotOpenCnt(bs.getOpenBoxTotal());
        return luckRepo.save(ls);
    }

    private Map<String, String> firstOrEmpty(List<Map<String, String>> list) {
        if (list == null || list.isEmpty()) return Map.of();
        return list.get(0);
    }

    private int luckEventDays() {
        try {
            var rows = luckCfg.other();
            if (rows != null && !rows.isEmpty()) {
                int days = pInt(rows.get(0).get("time"), 7);
                return Math.max(0, days);
            }
        } catch (Exception e) {
            log.debug("[box] luckCfg.other parse failed: {}", e.toString());
        }
        return 7;
    }

    private Optional<Map<String, Object>> luckRewardBySeq(int seq) {
        var rows = luckCfg.reward();
        if (rows == null || rows.isEmpty()) return Optional.empty();

        for (var r : rows) {
            if (pInt(r.get("type"), -1) == seq) {
                return Optional.of(r);
            }
        }
        if (seq >= 0 && seq < rows.size()) {
            return Optional.of(rows.get(seq));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObjMap(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }


    private int pickEquipPart() {
        // Ở equipment.json có part 0..9; chọn uniform (có thể thay bằng bảng trọng số nếu bạn thêm vào config)
        return ThreadLocalRandom.current().nextInt(10);
    }

    /** Bản pickInt “chịu khó” (nên dùng thay cho bản cũ) */
    private Integer pickInt(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;
            try {
                if (v instanceof Number n) {
                    long lv = (long) Math.floor(n.doubleValue());
                    if (lv > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    if (lv < Integer.MIN_VALUE) return Integer.MIN_VALUE;
                    return (int) lv;
                }
                String s = String.valueOf(v).trim();
                if (s.isEmpty() || "null".equalsIgnoreCase(s)) continue;
                int dot = s.indexOf('.');
                if (dot > 0) s = s.substring(0, dot);
                return Integer.parseInt(s);
            } catch (Exception ignore) {}
        }
        return null;
    }

    // ========= WaBao SC Data Methods (SC 1643/1645/1646/1647/1648/1651) =========

    /** SC 1643 PB_SCWaBaoMapInfo */
    public BoxDTOs.WaBaoMapInfo getWaBaoMapInfo(Long roleId) {
        BoxState s = getOrCreate(roleId);
        int level = s.getBoxLevel();
        List<Integer> conditions = new ArrayList<>();
        try {
            var rows = unpackCfg.fixedReward();
            if (rows != null && !rows.isEmpty()) conditions.add(rows.size());
        } catch (Exception ignore) {}
        return BoxDTOs.WaBaoMapInfo.builder()
                .curMap(level).unlockedMap(level).mapConditionNum(conditions).build();
    }

    /** SC 1645 PB_SCWaBaoIntegrityInfo */
    public BoxDTOs.WaBaoIntegrityInfo getWaBaoIntegrity(Long roleId) {
        return BoxDTOs.WaBaoIntegrityInfo.builder().isLogin(1).dataList(List.of()).build();
    }

    /** SC 1646 PB_SCWaBaoCollectionListInfo */
    public BoxDTOs.WaBaoCollectionInfo getWaBaoCollection(Long roleId) {
        return BoxDTOs.WaBaoCollectionInfo.builder().isLogin(1).dataList(List.of()).build();
    }

    /** SC 1647 PB_SCWaBaoToolInfo */
    public BoxDTOs.WaBaoToolInfo getWaBaoToolInfo(Long roleId) {
        return BoxDTOs.WaBaoToolInfo.builder().toolList(List.of()).build();
    }

    /** SC 1648 PB_SCWaBaoTaskInfo
     *  proto: task_flag(int32), repeated int32 task_list, repeated int32 task_type_num
     *  task_list   = taskId list
     *  task_type_num = progress/count per task (same index)
     */
    public BoxDTOs.WaBaoTaskInfo getWaBaoTaskInfo(Long roleId) {
        BoxState s = getOrCreate(roleId);
        int opened = s.getOpenBoxTotal();
        int target = 10;
        int status = opened >= target ? 1 : 0;  // 0=ongoing 1=claimable
        return BoxDTOs.WaBaoTaskInfo.builder()
                .taskFlag(0)
                .taskList(List.of(1))                         // taskId=1
                .taskTypeNumList(List.of(Math.min(opened, target))) // progress
                .build();
    }

    /** SC 1651 PB_SCWaBaoBookListInfo
     *  proto: repeated int32 activate_flag  (bit-flags per book)
     */
    public BoxDTOs.WaBaoBookListInfo getWaBaoBookListInfo(Long roleId) {
        return BoxDTOs.WaBaoBookListInfo.builder()
                .activateFlagList(List.of())
                .build();
    }
}

