package com.SouthMillion.box_service.service;

import com.SouthMillion.box_service.config.EquipmentIndex;
import com.SouthMillion.box_service.config.LuckUnpackConfigCache;
import com.SouthMillion.box_service.config.UnpackConfigCache;
import com.SouthMillion.box_service.enity.BoxSetting;
import com.SouthMillion.box_service.enity.BoxState;
import com.SouthMillion.box_service.enity.LuckState;
import com.SouthMillion.box_service.repository.BoxSettingRepository;
import com.SouthMillion.box_service.repository.BoxStateRepository;
import com.SouthMillion.box_service.repository.LuckStateRepository;
import com.SouthMillion.box_service.service.client.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.bag.BagDTOs.ItemDelta;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
@RequiredArgsConstructor
public class BoxService {

    // ========= DEPENDENCIES =========
    private final BoxStateRepository boxRepo;
    private final LuckStateRepository luckRepo;
    private final BoxSettingRepository settingRepo;

    private final UnpackConfigCache unpackCfg;
    private final LuckUnpackConfigCache luckCfg;
    private final EquipmentIndex equipIdx;                // NO hard-code
    private final BagFeign bag;
    private final ItemMetaFeign itemFeign;                // meta để lọc virtual
    private final EquipFeign equipFeign;
    private final RoleFeign roleFeign;
    private final ObjectMapper om = new ObjectMapper();

    // ========= CONSTANTS =========
    private static final byte BAG_COMMON = 0;
    private static final byte BAG_EQUIP = 1;

    private static final int SRC_MSG_BOX = 3000;
    private static final int SRC_OP_OPEN = 3001;
    private static final int SRC_OP_REWARD = 3002;
    private static final int SRC_OP_BUY = 3003;
    private static final int SRC_OP_LEVEL = 3004;
    private static final int SRC_OP_CONSUME = 3005;


    // ========= PUBLIC APIS =========

    public BoxDTOs.InfoResp info(String roleId) {
        var s = getOrCreate(roleId);
        maybeCompleteLevelUp(s);
        Map<String, Object> pending = parsePendingJsonSafe(s.getPendingJson());

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
        String roleId = req.getRoleId();
        int count = Math.max(1, Math.min(5, req.getCount()));
        boolean isFive = count >= 5;

        var s = getOrCreate(roleId);
        maybeCompleteLevelUp(s);
        maybeDailyReset(s);

        long nowSec = Instant.now().getEpochSecond();

        Map<String, Object> pend = parsePendingJsonSafe(s.getPendingJson());
        boolean hasPendingEquip = pend != null && "equip".equals(String.valueOf(pend.get("kind")));
        if (hasPendingEquip) {
            return BoxDTOs.OpenResp.builder()
                    .pending(pend)
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
                    .pending(pend)
                    .openBoxTotal(s.getOpenBoxTotal())
                    .lastOpenIsFive(s.isLastOpenIsFive())
                    .bonusItems(List.of())
                    .build();
        }

        // Unpack config
        Map<String, String> other = firstOrEmpty(unpackCfg.other());
        int boxItemId = pInt(other.get("unpack_item_id"), 0);
        if (boxItemId <= 0) throw new IllegalStateException("Bad config: other.unpack_item_id");

        // Fixed step?
        int step = nextFixedStep(s.getOpenBoxTotal(), isFive);
        long need = (step > 0) ? step : (isFive ? 5L : 1L);

        // Consume box item
        bag.consume(new BagDTOs.ConsumeReq(
                roleId, BAG_COMMON,
                List.of(new ItemDelta(boxItemId, need, false, "BOX_OPEN")),
                SRC_MSG_BOX, SRC_OP_CONSUME
        ));

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

                    s.setPendingJson(writeJson(pending));
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
                            .build();
                } else {
                    // Non-equip fixed → add vào túi thường
                    addNonVirtualItems(roleId,
                            List.of(new ItemDelta(fixedItemId, 1, false, "BOX_FIXED")),
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
                        List.of(new ItemDelta(fid, 1, false, "BOX_FASHION")),
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

        // Bonus reward theo colorRow (nếu có) — lọc virtual qua meta
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reward = (List<Map<String, Object>>) colorRow.get("reward");
        if (reward != null) {
            List<ItemDelta> add = new ArrayList<>();
            for (var r : reward) {
                int rid = pInt(r.get("item_id"), 0);
                long num = pLong(r.get("num"), 0L);
                if (rid > 0 && num > 0) {
                    add.add(new ItemDelta(rid, num, false, "BOX_COLOR_REWARD"));
                    bonus.add(bonusItem(rid, num, "colorReward"));
                }
            }
            addNonVirtualItems(roleId, add, BAG_COMMON, SRC_OP_REWARD);
        }

        s.setPendingJson(writeJson(pending));
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
                .build();
    }

    /**
     * Mặc equip (tối thiểu): mint item vào BAG_EQUIP rồi clear pending.
     */
    @Transactional
    public BoxDTOs.OkResp wear(String roleId) {
        var s = getOrCreate(roleId);
        Map<String, Object> pend = parsePendingJsonSafe(s.getPendingJson());
        if (pend == null || !"equip".equals(String.valueOf(pend.get("kind")))) {
            return BoxDTOs.OkResp.builder().ok(false).message("NO_PENDING").build();
        }
        int itemId = pInt(pend.get("itemId"), 0);
        if (itemId <= 0) return BoxDTOs.OkResp.builder().ok(false).message("BAD_ITEM").build();

        addNonVirtualItems(roleId,
                List.of(new ItemDelta(itemId, 1, false, "BOX_WEAR")),
                BAG_EQUIP, SRC_OP_REWARD);

        s.setPendingJson(null);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    /**
     * Bán equip (tối thiểu): clear pending. (Coin nên do wallet-service xử lý; ở đây không cộng tiền ảo)
     */
    @Transactional
    public BoxDTOs.OkResp sell(String roleId) {
        BoxState s = getOrCreate(roleId);
        s.setPendingJson(null);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    /**
     * Mua lần mở (demo): tăng boxBuyTimes.
     */
    @Transactional
    public BoxDTOs.OkResp buy(String roleId) {
        BoxState s = getOrCreate(roleId);
        setSafeIntField(s, "boxBuyTimes", s.getBoxBuyTimes() + 1);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    /**
     * Nâng cấp hộp (demo): set timer 60s.
     */
    @Transactional
    public BoxDTOs.OkResp levelUp(String roleId) {
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
    public BoxDTOs.OkResp levelReward(String roleId, int idx) {
        BoxState s = getOrCreate(roleId);
        s.setLevelFetchFlag(s.getLevelFetchFlag() | (1 << Math.max(0, Math.min(30, idx))));
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    // ========= LUCK (tối thiểu) =========

    public BoxDTOs.LuckInfoResp luckInfo(String roleId) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> snapshotLuckOpen(roleId, 0));
        return BoxDTOs.LuckInfoResp.builder()
                .endTimestamp(ls.getEndEpoch())
                .receiveFlag(ls.getReceiveBitmap())
                .openBoxNumDelta(Math.max(0, getOrCreate(roleId).getOpenBoxTotal() - ls.getSnapshotOpenCnt()))
                .boxLevel(getOrCreate(roleId).getBoxLevel())
                .build();
    }

    @Transactional
    public BoxDTOs.OkResp luckReceive(String roleId, int seq) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> snapshotLuckOpen(roleId, 0));
        long now = Instant.now().getEpochSecond();
        if (ls.getEndEpoch() > 0 && ls.getEndEpoch() <= now)
            return BoxDTOs.OkResp.builder().ok(false).message("LUCK_ENDED").build();

        // Demo: cộng 1 item tượng trưng & đánh dấu đã nhận (lọc virtual)
        addNonVirtualItems(roleId, List.of(new ItemDelta(1000, 1, false, "LUCK_REWARD")), BAG_COMMON, SRC_OP_REWARD);
        ls.setReceiveBitmap(ls.getReceiveBitmap() | (1L << Math.max(0, Math.min(61, seq))));
        luckRepo.save(ls);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    @Transactional
    public BoxDTOs.OkResp quicken(String roleId, int num) {
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
            bag.consume(new BagDTOs.ConsumeReq(roleId, (byte) 0,
                    List.of(new ItemDelta(quickId, num, false, null)), 3201, 0));
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
    public BoxDTOs.BoxSettingResp getSetting(String roleId) {
        var s = settingRepo.findById(roleId).orElseGet(() -> {
            var ns = new BoxSetting();
            ns.setRoleId(roleId);
            ns.setEquipCapMark(1);
            return settingRepo.save(ns);
        });
        return mapSetting(s);
    }

    public BoxDTOs.BoxSettingResp saveSetting(String roleId, BoxDTOs.BoxSettingResp req) {
        var s = settingRepo.findById(roleId).orElseGet(() -> {
            var ns = new BoxSetting();
            ns.setRoleId(roleId);
            return ns;
        });
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
    public BoxDTOs.DecomposeResp decompose(String roleId) {
        BoxState s = getOrCreate(roleId);

        Map<String, Object> pend = parsePendingJsonSafe(s.getPendingJson());
        if (pend == null || !"equip".equals(String.valueOf(pend.get("kind")))) {
            return BoxDTOs.DecomposeResp.builder().ok(false).message("No pending equip").build();
        }

        Map<String, Object> req = Map.of(
                "roleId", roleId,
                "item", pend
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

        if (s.isLastOpenIsFive() && Boolean.TRUE.equals(pend.get("isNew"))) {
            if (gotNum > 0) gotNum *= 5;
            if (gotExp > 0) gotExp *= 5;
        }

        if (gotItemId > 0 && gotNum > 0) {
            bag.add(new BagDTOs.AddItemReq(roleId, (byte) 0,
                    List.of(new ItemDelta(gotItemId, gotNum, false, "BOX_DECOMPOSE_ITEM")), 3601, 0));
        }
        if (gotExp > 0) {
            try {
                roleFeign.addExp(new RoleDTOs.AddExpReq(roleId, gotExp));
            } catch (Exception e) {
                log.warn("roleFeign.addExp failed: {}", e.getMessage());
            }
        }

        s.setPendingJson(null);
        s.setLastOpenIsFive(false);
        boxRepo.save(s);

        return BoxDTOs.DecomposeResp.builder()
                .ok(true)
                .gotItemId(gotItemId)
                .gotNum(gotNum)
                .gotExp(gotExp)
                .message("Decomposed")
                .build();
    }

    // ========= INTERNAL HELPERS =========

    private BoxState getOrCreate(String roleId) {
        return boxRepo.findById(roleId).orElseGet(() -> {
            BoxState s = new BoxState();
            s.setRoleId(roleId);
            s.setBoxLevel(1);
            s.setBoxBuyTimes(0);
            s.setLevelUpEndEpoch(0);
            s.setLevelFetchFlag(0);
            s.setOpenBoxTotal(0);
            s.setLastOpenIsFive(false);
            s.setPendingJson(null);
            // optional fields (shiZhuangNum, arenaItemNum) nếu entity có
            trySetIntField(s, "shiZhuangNum", 0);
            trySetIntField(s, "arenaItemNum", 0);
            trySetLongField(s, "lastOpenEpoch", 0L);
            return boxRepo.save(s);
        });
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
        // Demo: bỏ qua hoặc reset theo ngày nếu bạn có cột updatedAt; ở đây để trống cho an toàn
    }

    private Map<String, Object> parsePendingJsonSafe(String json) {
        if (!StringUtils.hasText(json)) return null;
        String s = json.trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "{}".equals(s)) return null;
        try {
            return om.readValue(s, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            // Trả về raw để debug format cũ
            return Map.of("_raw", json);
        }
    }

    private String writeJson(Map<String, Object> m) {
        try {
            return om.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("writeJson pending fail", e);
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
        // Có thể lọc theo boxLevel nếu config có cột này
        if (rows == null || rows.isEmpty()) return Map.of();
        return rows.get(ThreadLocalRandom.current().nextInt(rows.size()));
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

    private int fetchPlayerLevel(String roleId, int hintedLevel) {
        // Tối thiểu: dùng hintedLevel từ req
        return Math.max(1, hintedLevel);
    }

    private void rollArenaTicketIfAny(String roleId, BoxState s, List<Map<String, Object>> bonus, Map<String, String> other) {
        // TODO: nếu game có vé đấu trường → add vào bonus & bag
    }

    // ========= BAG helpers =========

    /** Chỉ add vật phẩm **không-ảo** vào bag (tránh lỗi VIRTUAL_ITEM_USE_WALLET). */
    /**
     * Lọc virtual bằng ItemFeign + knowledge từ equipment.json.
     */
    private void addNonVirtualItems(String roleId, List<BagDTOs.ItemDelta> items, byte bagType, int srcOp) {
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
                Map<String, Map<String, Object>> metas = itemFeign.batchMeta(csv);
                for (var it : unknown) {
                    var m = metas.get(String.valueOf(it.getItemId()));
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
                bag.add(new BagDTOs.AddItemReq(roleId, bagType, finalAdd, SRC_MSG_BOX, srcOp));
            } catch (Throwable t) {
                log.warn("[box] bag.add error rid={} ex={}", roleId, t.toString());
            }
        }
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
    private LuckState snapshotLuckOpen(String roleId, int days) {
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


    private int pickEquipPart() {
        // Ở equipment.json có part 0..9; chọn uniform (có thể thay bằng bảng trọng số nếu bạn thêm vào config)
        return ThreadLocalRandom.current().nextInt(10);
    }

    @Transactional
    public void savePending(String roleId, Map<String, Object> pending) {
        BoxState s = getOrCreate(roleId);

        Map<String, Object> canonical = canonicalizePendingEquip(pending);
        try {
            if (canonical == null) {
                // Không hợp lệ thì xóa pending (an toàn)
                s.setPendingJson(null);
            } else {
                s.setPendingJson(om.writeValueAsString(canonical));
            }
            boxRepo.save(s);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot persist pendingJson: " + e.getMessage(), e);
        }
    }

    /**
     * Chuẩn hóa về {"kind":"equip","equip":{"itemId":..,"equipType":..}}
     * Hỗ trợ cả dạng phẳng hoặc lồng {equip:{...}} và nhiều alias key.
     * Nếu thiếu itemId -> trả null để bỏ.
     */
    private Map<String, Object> canonicalizePendingEquip(Map<String, Object> pending) {
        if (pending == null || pending.isEmpty()) return null;

        Map<String, Object> src = pending;
        Object nested = pending.get("equip");
        if (nested instanceof Map<?,?> m) {
            @SuppressWarnings("unchecked") Map<String,Object> mm = (Map<String,Object>) m;
            src = mm;
        }

        Integer itemId   = pickInt(src, "itemId", "item_id", "id");
        Integer equipTyp = pickInt(src, "equipType", "equip_type", "type", "pos");

        if (itemId == null || itemId <= 0) return null;
        if (equipTyp == null) equipTyp = 1;

        Map<String, Object> equip = new HashMap<>();
        equip.put("itemId", itemId);
        equip.put("equipType", equipTyp);

        Map<String, Object> out = new HashMap<>();
        out.put("kind", "equip");
        out.put("equip", equip);
        return out;
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
}