package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.config.AttrCalculator;
import com.SouthMillion.pet_service.config.PetConfigCache;
import com.SouthMillion.pet_service.entity.PetFightEntity;
import com.SouthMillion.pet_service.repository.PetFightRepository;
import com.SouthMillion.pet_service.repository.PetRoleRepository;
import com.SouthMillion.pet_service.service.client.BagFeign;
import com.SouthMillion.pet_service.service.client.WalletFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.pet.PetDTOs;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PetService (MVP)
 * - /api/pet/info
 * - /api/pet/set-fight
 * - /api/pet/level-up (ưu tiên item EXP; nếu không có, trừ coin theo config)
 *
 * Tất cả "magic numbers/strings" đều lấy từ application.yml qua @Value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    // ===== Dep inject =====
    private final PetRoleRepository roleRepo;
    private final PetFightRepository fightRepo;
    private final PetConfigCache cfg;
    private final BagFeign bag;
    private final WalletFeign wallet;

    // ===== Configurable via application.yml (NO hard-code) =====
    /** Túi dùng để tiêu item nâng cấp thú (0 = common bag). */
    @Value("${app.pet.bagType:0}")
    private byte petBagType;

    /** Reason string cho Bag consume/add khi LEVEL_UP. */
    @Value("${app.pet.levelUp.reason.string:PET_LEVEL_UP}")
    private String reasonStrLevelUp;

    /** Reason code cho Wallet khi LEVEL_UP. */
    @Value("${app.pet.levelUp.reason.code:201}")
    private int reasonCodeLevelUp;

    /** srcMsgId ghi kèm khi thao tác với Bag (audit). */
    @Value("${app.pet.levelUp.srcMsgId:0}")
    private int srcMsgIdLevelUp;

    /** srcOp ghi kèm khi thao tác với Bag (audit). */
    @Value("${app.pet.levelUp.srcOp:201}")
    private int srcOpLevelUp;

    /** ItemId của “coin” dùng khi không dùng item EXP (vd 40001). */
    @Value("${app.pet.wallet.coinItemId:40001}")
    private long walletCoinItemId;

    /** IdemKey prefix cho các giao dịch ví khi level-up (tùy chọn). */
    @Value("${app.pet.levelUp.idemKeyPrefix:PET_LVUP}")
    private String idemKeyPrefix;

    /** Max level fallback nếu không có trong config other.pet_level_max. */
    @Value("${app.pet.level.max:300}")
    private int levelMaxFallback;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===================== Views =====================

    public PetDTOs.AllInfoResp info(String roleId) {
        var list = roleRepo.findAllByRoleIdOrderByPetIndexAsc(roleId);
        var fight = fightRepo.findByRoleId(roleId).orElse(null);

        List<Integer> fightIdx = readIntList(fight != null ? fight.getFightIndexesJson() : null);

        List<PetDTOs.PetData> pets = new ArrayList<>();
        for (var pe : list) {
            var r = AttrCalculator.compute(pe, cfg);
            pets.add(PetDTOs.PetData.builder()
                    .petIndex(pe.getPetIndex())
                    .petId(pe.getPetId())
                    .petLevel(pe.getLevel())
                    .petExp(pe.getExp())
                    .petOrder(pe.getOrder())
                    .attrList(r.attrList)
                    .capability(r.capability)
                    .skillLockFlag(pe.getSkillLockFlag())
                    .build());
        }

        return PetDTOs.AllInfoResp.builder()
                .fightPetIndex(fightIdx)
                .petList(pets)
                .build();
    }

    // ===================== Commands =====================

    @Transactional
    public PetDTOs.OkResp setFight(PetDTOs.SetFightReq req) {
        final String roleId = req.roleId().trim();
        final List<Integer> idx = req.fightPetIndex() == null ? List.of() : req.fightPetIndex();

        var entity = fightRepo.findByRoleId(roleId).orElseGet(() ->
                PetFightEntity.builder().roleId(roleId).fightIndexesJson("[]").build());
        entity.setFightIndexesJson(writeIntList(idx));
        fightRepo.save(entity);
        return PetDTOs.OkResp.OK();
    }

    /**
     * LevelUp:
     *  - Nếu client gửi costItems (item EXP) -> tiêu trong túi
     *  - Ngược lại -> trừ coin (walletCoinItemId) theo "abandon" từng cấp
     */
    @Transactional
    public PetDTOs.OkResp levelUp(PetDTOs.LevelUpReq req) {
        final String roleId = req.roleId().trim();
        final int times = Math.max(1, req.times());

        var pe = roleRepo.findByRoleIdAndPetIndex(roleId, req.petIndex())
                .orElseThrow(() -> new IllegalArgumentException("pet not found"));

        // ---- Load config cần thiết ----
        var petBase = cfg.petBaseById().get(pe.getPetId());
        if (petBase == null) return PetDTOs.OkResp.NG("pet base not found");

        final int petType = asInt(petBase.get("pet_type"));
        final int maxLevel = getMaxLevelFromConfig();

        if (pe.getLevel() >= maxLevel) return PetDTOs.OkResp.NG("level reached max");

        var upListByType = cfg.petUpByType().getOrDefault(petType, List.of());

        // ---- Tính tổng EXP cần và targetLevel trong phạm vi times/maxLevel ----
        long needExp = 0L;
        int startLevel = pe.getLevel();
        int targetLevel = startLevel;

        for (int t = 0; t < times; t++) {
            int next = targetLevel + 1;
            if (next > maxLevel) break;
            Map<String,Object> row = findUpRow(upListByType, next);
            if (row == null) break;
            needExp += asLong(row.getOrDefault("up_exp", 0));
            targetLevel = next;
        }
        if (needExp <= 0) return PetDTOs.OkResp.NG("no levels to upgrade");

        // ---- Nhánh 1: dùng item EXP nếu client gửi costItems ----
        if (!CollectionUtils.isEmpty(req.costItems())) {
            long providedExp = calcExpFromItems(req.costItems());
            if (providedExp < needExp) {
                return PetDTOs.OkResp.NG("not enough exp items");
            }

            List<BagDTOs.ItemDelta> deltas = req.costItems().entrySet().stream()
                    .map(e -> BagDTOs.ItemDelta.builder()
                            .itemId(Math.toIntExact(e.getKey()))
                            .count(e.getValue())
                            .bound(false)
                            .reason(reasonStrLevelUp)
                            .build())
                    .toList();

            BagDTOs.ConsumeReq consume = BagDTOs.ConsumeReq.builder()
                    .roleId(roleId)
                    .bagType(petBagType)
                    .items(deltas)
                    .srcMsgId(srcMsgIdLevelUp)
                    .srcOp(srcOpLevelUp)
                    .build();

            var ok = bag.consume(consume);
            if (ok == null || !ok.ok()) {
                return PetDTOs.OkResp.NG(ok != null ? ok.getMessage() : "bag.consume failed");
            }
        } else {
            // ---- Nhánh 2: trừ coin theo từng cấp (abandon) ----
            long coinTotal = 0L;

            for (int lv = startLevel + 1; lv <= targetLevel; lv++) {
                Map<String, Object> row = findUpRow(upListByType, lv);
                if (row == null) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> abandons =
                        (List<Map<String, Object>>) row.getOrDefault("abandon", List.of());

                for (var a : abandons) {
                    long itemId = asLong(a.get("item_id"));
                    long num    = asLong(a.get("num"));
                    if (itemId == walletCoinItemId) coinTotal += num;
                }
            }

            if (coinTotal > 0) {
                WalletDTOs.BatchReq costReq = WalletDTOs.BatchReq.builder()
                        .roleId(roleId)
                        .changes(List.of(
                                WalletDTOs.Change.builder()
                                        .itemId(walletCoinItemId)
                                        .amount(coinTotal)
                                        .build()
                        ))
                        .idemKey(genIdemKey(roleId, pe.getPetIndex(), startLevel, targetLevel, coinTotal))
                        .reason(reasonCodeLevelUp)
                        .build();

                ResultDTO<WalletDTOs.MutateResp> res = wallet.batchCost(costReq);
                if (res == null || res.getData() == null || !res.getData().ok()) {
                    return PetDTOs.OkResp.NG(
                            res != null && res.getData() != null ? res.getData().error() : "wallet.cost failed"
                    );
                }
            }
        }

        // ---- Áp tăng level/exp ----
        pe.setLevel(targetLevel);
        pe.setExp(0L); // MVP: reset exp; nếu giữ dư exp thì sửa theo rule của bạn
        roleRepo.save(pe);

        return PetDTOs.OkResp.OK();
    }

    // ===================== Helpers =====================

    private String genIdemKey(String roleId, int petIndex, int startLv, int targetLv, long coinTotal) {
        // idemKey = prefix:role:idx:start->target:coin
        return String.format("%s:%s:%d:%d->%d:%d", idemKeyPrefix, roleId, petIndex, startLv, targetLv, coinTotal);
    }

    private int getMaxLevelFromConfig() {
        Object v = cfg.other().get("pet_level_max");
        int fromCfg = asInt(v);
        return fromCfg > 0 ? fromCfg : levelMaxFallback;
    }

    private static Map<String,Object> findUpRow(List<Map<String,Object>> upList, int level) {
        for (var r : upList) {
            int lv = asInt(r.get("pet_level"));
            if (lv == level) return r;
        }
        return null;
    }

    /**
     * Tính tổng EXP có được từ map (itemId -> count) theo other.pet_exp_item_i / other.pet_exp_i
     * - không hard-code số item: quét dải i=0..9 (đủ linh hoạt cho hầu hết trường hợp)
     */
    private long calcExpFromItems(Map<Long, Long> items) {
        var other = cfg.other();
        Map<Long, Long> itemExp = new HashMap<>();
        for (int i = 0; i <= 9; i++) {
            long itemId = asLong(other.get("pet_exp_item_" + i));
            long expPer = asLong(other.get("pet_exp_" + i));
            if (itemId > 0 && expPer > 0) {
                itemExp.put(itemId, expPer);
            }
        }
        long total = 0L;
        for (var e : items.entrySet()) {
            Long per = itemExp.get(e.getKey());
            if (per != null) total += per * e.getValue();
        }
        return total;
    }

    private static int asInt(Object o){
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        return 0;
    }
    private static long asLong(Object o){
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s && !s.isBlank()) return Long.parseLong(s.trim());
        return 0L;
    }

    private static List<Integer> readIntList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String writeIntList(List<Integer> list) {
        try {
            return MAPPER.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }
}