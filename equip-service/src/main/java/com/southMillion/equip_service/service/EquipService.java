package com.southMillion.equip_service.service;


import com.southMillion.equip_service.config.EquipProperties;
import com.southMillion.equip_service.entity.EquipSlotEntity;
import com.southMillion.equip_service.repository.EquipSlotRepository;
import com.southMillion.equip_service.service.client.BagInternalFeign;
import com.southMillion.equip_service.service.client.ItemMetaFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class EquipService {

    private final EquipSlotRepository slotRepo;
    private final ItemMetaFeign itemMetaFeign;
    private final BagInternalFeign bagFeign;
    private final EquipProperties props;

    // ================= PUBLIC (khớp /api/equip) =================

    public EquipDTOs.ListResp list(String roleId) {
        var list = slotRepo.findByRoleId(roleId);
        var items = new ArrayList<EquipDTOs.EquipItem>();
        for (var e : list) items.add(toEquipItem(e));
        return new EquipDTOs.ListResp(items);
    }

    @Transactional
    public EquipDTOs.OkResp equip(EquipDTOs.EquipReq req) {
        // 1) lấy meta để biết equipType
        var meta = getOneMeta(req.getItemId());
        int equipType = extractEquipType(meta);
        if (equipType < 0) equipType = 0; // chuẩn hoá
        if (equipType == 0) return EquipDTOs.OkResp.NG("ITEM_NOT_EQUIPPABLE");

        // 2) trừ item trong túi
        var consume = new BagDTOs.ConsumeReq(
                req.getRoleId(),
                req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                List.of(new BagDTOs.ItemDelta(req.getItemId(), 1, false, "equip")),
                1600, 1
        );
        var ok = bagFeign.consume(consume);
        if (ok == null || !ok.ok()) return EquipDTOs.OkResp.NG("ITEM_NOT_ENOUGH");

        // 3) lấy/khởi tạo slot
        int finalEquipType = equipType;
        var slot = slotRepo.findByRoleIdAndEquipType(req.getRoleId(), equipType)
                .orElseGet(() -> {
                    var s = new EquipSlotEntity();
                    s.setRoleId(req.getRoleId());
                    s.setEquipType(finalEquipType);
                    s.setItemId(0);
                    return s;
                });

        // 4) nếu có đồ cũ -> trả về túi
        if (slot.getItemId() > 0) {
            var add = new BagDTOs.AddItemReq(
                    req.getRoleId(),
                    req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                    List.of(new BagDTOs.ItemDelta(slot.getItemId(), 1, false, "unequip")),
                    1600, 2
            );
            var addResp = bagFeign.add(add);
            if (addResp == null || addResp.getAdded() == null) {
                log.warn("Return old equip to bag failed role={}, item={}", req.getRoleId(), slot.getItemId());
            }
        }

        // 5) snapshot stats & lưu slot
        snapshotStatsFromMeta(slot, meta);
        slot.setItemId(req.getItemId());
        slotRepo.save(slot);

        return EquipDTOs.OkResp.OK();
    }

    @Transactional
    public EquipDTOs.OkResp unequip(EquipDTOs.UnequipReq req) {
        var slotOpt = slotRepo.findByRoleIdAndEquipType(req.getRoleId(), req.getEquipType());
        if (slotOpt.isEmpty() || slotOpt.get().getItemId() <= 0) {
            return EquipDTOs.OkResp.NG("SLOT_EMPTY");
        }
        var slot = slotOpt.get();

        // 1) thêm item về túi
        var add = new BagDTOs.AddItemReq(
                req.getRoleId(),
                req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                List.of(new BagDTOs.ItemDelta(slot.getItemId(), 1, false, "unequip")),
                1600, 3
        );
        var addResp = bagFeign.add(add);
        if (addResp == null || addResp.getAdded() == null) return EquipDTOs.OkResp.NG("BAG_ADD_FAILED");

        // 2) clear slot
        clearSlot(slot);
        slotRepo.save(slot);

        return EquipDTOs.OkResp.OK();
    }

    // NEW: wrapper wear-by-itemId (được gọi từ /wear/{roleId}/{itemId})
    @Transactional
    public EquipDTOs.OkResp wear(String roleId, int itemId, Integer bagType) {
        var req = new EquipDTOs.EquipReq();
        req.setRoleId(roleId);
        req.setItemId(itemId);
        if (bagType != null) req.setBagType(bagType.byteValue()); // nếu null -> service dùng props/equip bag type
        return equip(req);
    }


    // ================= INTERNAL (khớp /internal/equip) =================

    /**
     * Mặc món pending từ Box:
     * - KHÔNG đụng tới túi.
     * - Ghi món mới vào slot.
     * - Trả về "replaced" (món cũ nếu có) dạng spec Map để BoxService đưa vào pendingJson (isNew=false).
     */
    @Transactional
    public Map<String, Object> wearFromBox(Map<String, Object> req) {
        String roleId = String.valueOf(req.get("roleId"));
        @SuppressWarnings("unchecked")
        Map<String,Object> item = (Map<String,Object>) req.get("item");
        int itemId = asInt(item.get("itemId"), 0);

        // Xác định equipType & meta
        var metaNew = getOneMeta(itemId);
        int equipType = firstNonZero(
                asInt(item.get("equipType"), 0),
                extractEquipType(metaNew)
        );
        if (equipType == 0) throw new IllegalArgumentException("ITEM_NOT_EQUIPPABLE");

        // slot hiện tại
        var slot = slotRepo.findByRoleIdAndEquipType(roleId, equipType)
                .orElseGet(() -> {
                    var s = new EquipSlotEntity();
                    s.setRoleId(roleId);
                    s.setEquipType(equipType);
                    s.setItemId(0);
                    return s;
                });

        // Build "replaced" (nếu có)
        Map<String,Object> replaced = null;
        if (slot.getItemId() > 0) {
            var metaOld = getOneMeta(slot.getItemId());
            replaced = new LinkedHashMap<>();
            replaced.put("itemId", slot.getItemId());
            replaced.put("equipType", slot.getEquipType());
            replaced.put("quality", extractQuality(metaOld, /*fallback*/1));
            // equipLevel: nếu pending có level → giữ lại level cũ nếu metaOld không có
            int oldLevel = extractLevel(metaOld, asInt(item.get("equipLevel"), 1));
            replaced.put("equipLevel", oldLevel);
        }

        // Ghi món mới vào slot (snapshot stats)
        snapshotStatsFromMeta(slot, metaNew);
        slot.setItemId(itemId);
        slotRepo.save(slot);

        return Map.of("replaced", replaced);
    }

    /**
     * Tính coin/exp khi bán equip.
     * - Đọc meta để lấy các khóa như: sell_price / sell_exp (nếu có).
     * - Nếu request có "businessmanPermyriad" (0..10000) -> áp dụng vào coin.
     * - Nếu meta thiếu, dùng công thức fallback mềm theo quality/level.
     */
    public Map<String, Object> computeSell(Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        Map<String,Object> item = (Map<String,Object>) req.get("item");
        int itemId     = asInt(item.get("itemId"), 0);
        int quality    = asInt(item.get("quality"), 1);
        int equipLevel = asInt(item.get("equipLevel"), 1);

        long businessman = asLong(req.get("businessmanPermyriad"), 0L); // optional

        var meta = getOneMeta(itemId);
        long baseCoin = firstNonZeroL(
                asLong(meta.get("sell_price"), 0L),
                asLong(meta.get("price_sell"), 0L),
                asLong(meta.get("price"), 0L)
        );
        long baseExp = firstNonZeroL(
                asLong(meta.get("sell_exp"), 0L),
                asLong(meta.get("exp_sell"), 0L)
        );

        if (baseCoin <= 0) baseCoin = fallbackSellCoin(quality, equipLevel);
        if (baseExp  <= 0) baseExp  = fallbackSellExp(quality, equipLevel);

        // hệ số businessman: coin * (1 + permyriad/10000)
        if (businessman > 0) {
            baseCoin = Math.round(baseCoin * (1.0 + (businessman / 10000.0)));
        }

        return Map.of("coin", baseCoin, "exp", baseExp);
    }

    /**
     * Phân giải:
     * - Đọc meta lấy { decompose_item_id, decompose_num_base, decompose_num_per_level, decompose_exp } nếu có.
     * - Nếu thiếu -> fallback mềm theo equipType/quality/level.
     * - KHÔNG thực thi add/consume; chỉ trả về kết quả để BoxService xử lý.
     */
    public Map<String, Object> decompose(Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        Map<String,Object> item = (Map<String,Object>) req.get("item");
        int itemId     = asInt(item.get("itemId"), 0);
        int quality    = asInt(item.get("quality"), 1);
        int equipLevel = asInt(item.get("equipLevel"), 1);

        var meta = getOneMeta(itemId);

        int itemOut = firstNonZero(
                asInt(meta.get("decompose_item_id"), 0),
                props.getDecomposeItemId() // cho phép cấu hình 1 item chung qua properties
        );

        long numBase = firstNonZeroL(
                asLong(meta.get("decompose_num_base"), 0L),
                props.getDecomposeNumBase()
        );
        long numPerL = firstNonZeroL(
                asLong(meta.get("decompose_num_per_level"), 0L),
                props.getDecomposeNumPerLevel()
        );
        long expOut = firstNonZeroL(
                asLong(meta.get("decompose_exp"), 0L),
                fallbackSellExp(quality, equipLevel) // theo C++, exp phân giải ~= exp bán
        );

        long num = numBase + numPerL * Math.max(0, equipLevel - 1);
        if (num <= 0 && itemOut > 0) {
            // fallback rất nhẹ nếu meta hoàn toàn thiếu
            num = Math.max(1, quality);
        }

        return Map.of(
                "itemId", itemOut,
                "num", num,
                "exp", expOut
        );
    }

    /** Kiểm tra item có phải equip hay không. */
    public Map<String, Object> itemKind(Map<String, Object> req) {
        int itemId = asInt(req.get("itemId"), 0);
        var meta = getOneMeta(itemId);
        boolean equip = extractEquipType(meta) != 0 || hasAny(meta, "equipType", "equip_type", "position", "pos");
        return Map.of("equip", equip);
    }

    /** Cố gắng resolve itemId theo (equipType, quality, level). Nếu không thể, trả rỗng (để BoxService fallback). */
    public Map<String, Object> resolveItemId(Map<String, Object> req) {
        int equipType = asInt(req.get("equipType"), 0);
        int quality   = asInt(req.get("quality"), 1);
        int level     = asInt(req.get("level"), 1);

        // Nếu ItemMetaFeign của bạn có API tìm kiếm, gọi ở đây. Nếu không, trả rỗng.
        try {
            // Ví dụ giả định:
            // Integer resolved = itemMetaFeign.resolveEquip(equipType, quality, level).getItemId();
            // if (resolved != null && resolved > 0) return Map.of("itemId", resolved);
        } catch (Exception ignore) {}

        // Không tìm được -> trả empty để BoxService dùng fallback icon
        return Map.of();
    }

    /** Trả meta rút gọn (equipType/quality/...) để BoxService tham chiếu. */
    public Map<String, Object> itemMeta(Map<String, Object> req) {
        int itemId = asInt(req.get("itemId"), 0);
        var meta = getOneMeta(itemId);
        int equipType = extractEquipType(meta);
        int quality   = extractQuality(meta, 1);
        int level     = extractLevel(meta, 1);
        Map<String,Object> out = new LinkedHashMap<>();
        if (equipType != 0) out.put("equipType", equipType);
        if (quality   >  0) out.put("quality", quality);
        if (level     >  0) out.put("level", level);
        return out;
    }

    // ================= Helpers =================

    private EquipDTOs.EquipItem toEquipItem(EquipSlotEntity e) {
        return EquipDTOs.EquipItem.builder()
                .equipType(e.getEquipType())
                .itemId(e.getItemId())
                .hp(e.getHp())
                .attack(e.getAttack())
                .defend(e.getDefend())
                .speed(e.getSpeed())
                .attrType1(e.getAttrType1())
                .attrValue1(e.getAttrValue1())
                .attrType2(e.getAttrType2())
                .attrValue2(e.getAttrValue2())
                .build();
    }

    private void clearSlot(EquipSlotEntity slot) {
        slot.setItemId(0);
        slot.setHp(0); slot.setAttack(0); slot.setDefend(0); slot.setSpeed(0);
        slot.setAttrType1(0); slot.setAttrValue1(0);
        slot.setAttrType2(0); slot.setAttrValue2(0);
    }

    private Map<String,Object> getOneMeta(int itemId) {
        try {
            var map = itemMetaFeign.batchMeta(String.valueOf(itemId));
            return map == null ? Map.of() : map.getOrDefault(String.valueOf(itemId), Map.of());
        } catch (Exception e) {
            log.info("batchMeta failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private int extractEquipType(Map<String,Object> meta) {
        if (meta == null) return 0;
        Object v = firstNonNull(meta.get("equipType"), meta.get("equip_type"), meta.get("position"), meta.get("pos"));
        return asInt(v, 0);
    }

    private int extractQuality(Map<String,Object> meta, int def) {
        Object v = firstNonNull(meta.get("quality"), meta.get("color"), meta.get("q"));
        int q = asInt(v, def);
        return Math.max(1, q);
    }

    private int extractLevel(Map<String,Object> meta, int def) {
        Object v = firstNonNull(meta.get("level"), meta.get("lvl"), meta.get("lv"));
        return asInt(v, def);
    }

    private void snapshotStatsFromMeta(EquipSlotEntity s, Map<String,Object> meta) {
        s.setHp(intVal(meta, "hp"));
        s.setAttack(intVal(meta, "attack", "att"));
        s.setDefend(intVal(meta, "defend", "def"));
        s.setSpeed(intVal(meta, "speed", "spd"));
        s.setAttrType1(intVal(meta, "attrType1", "attr_type1", "fristAtt"));
        s.setAttrValue1(intVal(meta, "attrValue1", "attr_value1", "fristAttValue"));
        s.setAttrType2(intVal(meta, "attrType2", "attr_type2", "secondAtt"));
        s.setAttrValue2(intVal(meta, "attrValue2", "attr_value2", "secondAttValue"));
    }

    // ====== fallback công thức mềm (chỉ dùng khi meta không có) ======
    private long fallbackSellCoin(int quality, int equipLevel) {
        // ưu tiên lấy từ properties nếu có
        long base = Math.max(1, props.getSellCoinBase());
        long perQ = Math.max(0, props.getSellCoinPerQuality());
        long perL = Math.max(0, props.getSellCoinPerLevel());
        long coin = base + perQ * Math.max(1, quality) + perL * Math.max(1, equipLevel);
        if (coin > 0) return coin;
        // fallback tối thiểu
        return 100L * Math.max(1, quality) * Math.max(1, equipLevel / 10);
        // (BoxService sẽ nhân 5 nếu là mở-5 và isNew=true)
    }

    private long fallbackSellExp(int quality, int equipLevel) {
        long base = Math.max(0, props.getSellExpBase());
        long perQ = Math.max(0, props.getSellExpPerQuality());
        long perL = Math.max(0, props.getSellExpPerLevel());
        long exp = base + perQ * Math.max(1, quality) + perL * Math.max(1, equipLevel);
        if (exp > 0) return exp;
        return 10L * Math.max(1, quality) + 2L * Math.max(1, equipLevel);
    }

    // ====== small utils ======
    private static Object firstNonNull(Object... xs) { for (Object x : xs) if (x != null) return x; return null; }
    private static boolean hasAny(Map<String,Object> m, String... ks) {
        for (String k: ks) if (m.containsKey(k)) return true; return false;
    }
    private static int intVal(Map<String,Object> m, String... keys) {
        for (var k : keys) {
            Object v = m.get(k);
            if (v instanceof Number n) return n.intValue();
            if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception ignore){}
        }
        return 0;
    }
    private static int asInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        try { return v == null ? def : Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
    private static long asLong(Object v, long def) {
        if (v instanceof Number n) return n.longValue();
        try { return v == null ? def : Long.parseLong(v.toString()); } catch (Exception e) { return def; }
    }
    private static long firstNonZeroL(long... xs) { for (long x: xs) if (x != 0) return x; return 0; }
    private static int firstNonZero(int... xs) { for (int x: xs) if (x != 0) return x; return 0; }
}