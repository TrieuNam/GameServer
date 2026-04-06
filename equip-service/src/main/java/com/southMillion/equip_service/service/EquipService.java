package com.SouthMillion.equip_service.service;


import com.SouthMillion.equip_service.config.EquipProperties;
import com.SouthMillion.equip_service.config.EquipmentConfigCache;
import com.SouthMillion.equip_service.dto.WearableItemsResponse;
import com.SouthMillion.equip_service.entity.EquipSlotEntity;
import com.SouthMillion.equip_service.repository.EquipSnapshotRepository;
import com.SouthMillion.equip_service.repository.EquipSlotRepository;
import com.SouthMillion.equip_service.service.client.BagInternalFeign;
import com.SouthMillion.equip_service.service.client.BagPublicFeign;
import com.SouthMillion.equip_service.service.client.ItemMetaFeign;
import com.SouthMillion.equip_service.service.client.RoleFeign;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Slf4j
@Service
public class EquipService {

    private final EquipSlotRepository slotRepo;
    private final ItemMetaFeign itemMetaFeign;
    private final BagInternalFeign bagFeign;
    private final BagPublicFeign bagPublicFeign;
    private final EquipProperties props;
    private final EquipmentConfigCache equipmentConfigCache;
    private final RoleFeign roleFeign;
    private final EquipSnapshotRepository snapshotRepo;
    private final Counter metaFallbackCounter;

    // Virtual Thread executor for parallel operations
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public EquipService(EquipSlotRepository slotRepo,
                        ItemMetaFeign itemMetaFeign,
                        BagInternalFeign bagFeign,
                        BagPublicFeign bagPublicFeign,
                        EquipProperties props,
                        EquipmentConfigCache equipmentConfigCache,
                        RoleFeign roleFeign,
                        EquipSnapshotRepository snapshotRepo,
                        MeterRegistry meterRegistry) {
        this.slotRepo = slotRepo;
        this.itemMetaFeign = itemMetaFeign;
        this.bagFeign = bagFeign;
        this.bagPublicFeign = bagPublicFeign;
        this.props = props;
        this.equipmentConfigCache = equipmentConfigCache;
        this.roleFeign = roleFeign;
        this.snapshotRepo = snapshotRepo;
        this.metaFallbackCounter = meterRegistry.counter("equip.meta.fallback_used");
    }

    // ================= PUBLIC (khớp /api/equip) =================

    public EquipDTOs.ListResp list(Long roleId) {
        var list = slotRepo.findByRoleId(roleId);
        var items = new ArrayList<EquipDTOs.EquipItem>();
        for (var e : list) items.add(toEquipItem(e));
        return new EquipDTOs.ListResp(items);
    }

    public EquipDTOs.EquipItem snapshot(Long roleId, int equipType) {
        Optional<EquipDTOs.EquipItem> fromRedis = snapshotRepo.find(roleId, equipType);
        if (fromRedis.isPresent()) {
            return fromRedis.get();
        }

        Optional<EquipSlotEntity> slotOpt = slotRepo.findByRoleIdAndEquipType(roleId, equipType);
        if (slotOpt.isPresent() && slotOpt.get().getItemId() > 0) {
            EquipDTOs.EquipItem item = toEquipItem(slotOpt.get());
            snapshotRepo.save(roleId, equipType, item);
            return item;
        }
        return EquipDTOs.EquipItem.builder().equipType(equipType).itemId(0).build();
    }

    public boolean evictSnapshot(Long roleId, int equipType) {
        snapshotRepo.delete(roleId, equipType);
        return true;
    }

    public int evictSnapshotsByRole(Long roleId) {
        return snapshotRepo.deleteByRole(roleId);
    }

    public WearableItemsResponse listWearableItems(Long roleId) {
        List<BagDTOs.ItemView> bagItems;
        try {
            bagItems = bagPublicFeign.getBag(String.valueOf(roleId));
        } catch (Exception ex) {
            log.warn("Cannot read bag items for roleId {}: {}", roleId, ex.getMessage());
            return WearableItemsResponse.builder()
                    .roleId(String.valueOf(roleId))
                    .items(List.of())
                    .build();
        }

        if (bagItems == null || bagItems.isEmpty()) {
            return WearableItemsResponse.builder()
                    .roleId(String.valueOf(roleId))
                    .items(List.of())
                    .build();
        }

        Map<Integer, Map<String, Object>> metaByItemId = new HashMap<>();
        List<WearableItemsResponse.WearableItem> wearableItems = new ArrayList<>();

        for (BagDTOs.ItemView bagItem : bagItems) {
            if (bagItem == null || bagItem.getItemId() == null) continue;
            if (bagItem.getNum() == null || bagItem.getNum() <= 0) continue;

            Map<String, Object> meta = metaByItemId.computeIfAbsent(
                    bagItem.getItemId(),
                    this::getEquipMeta
            );

            int equipType = extractEquipType(meta);
            if (equipType < 0) continue;

            wearableItems.add(WearableItemsResponse.WearableItem.builder()
                    .bagItemId(bagItem.getId())
                    .itemId(bagItem.getItemId())
                    .num(bagItem.getNum())
                    .quality(bagItem.getQuality())
                    .equipType(equipType)
                    .build());
        }

        return WearableItemsResponse.builder()
                .roleId(String.valueOf(roleId))
                .items(wearableItems)
                .build();
    }

    @Transactional
    public EquipDTOs.OkResp equip(EquipDTOs.EquipReq req) {
        // 1) lấy meta để biết equipType
        var meta = getOneMeta(req.getItemId());
        int equipType = extractEquipType(meta);
        if (equipType < 0) return EquipDTOs.OkResp.NG("ITEM_NOT_EQUIPPABLE");

        // 2) trừ item trong túi
        var consume = createConsumeReq(req.getRoleId(), req.getItemId(), 1, "equip");
        var consumeResp = bagFeign.consume(consume);
        if (!isConsumeSuccess(consumeResp)) return EquipDTOs.OkResp.NG("ITEM_NOT_ENOUGH");

        // 3) lấy/khởi tạo slot
        int finalEquipType = equipType;
        Long roleIdLong = Long.parseLong(req.getRoleId());
        var slot = slotRepo.findByRoleIdAndEquipType(roleIdLong, equipType)
                .orElseGet(() -> {
                    var s = new EquipSlotEntity();
                    s.setRoleId(roleIdLong);
                    s.setEquipType(finalEquipType);
                    s.setItemId(0);
                    return s;
                });

        int oldHp = slot.getHp();
        int oldAtk = slot.getAttack();
        int oldDef = slot.getDefend();
        int oldSpd = slot.getSpeed();

        // 4) nếu có đồ cũ -> trả về túi & 5) snapshot stats & lưu slot
        // OPTIMIZATION: Run these in parallel
        int oldItemId = slot.getItemId();
        snapshotStatsFromMeta(slot, meta);
        slot.setItemId(req.getItemId());
        slotRepo.save(slot);
        syncSnapshot(roleIdLong, slot);

        // Parallel execution: Return old item to bag + Update role stats
        CompletableFuture<Void> returnOldFuture = CompletableFuture.completedFuture(null);
        if (oldItemId > 0) {
            returnOldFuture = CompletableFuture.runAsync(() -> {
                var add = createAddItemReq(req.getRoleId(), oldItemId, 1, false, "unequip-old", byteToInteger(req.getBagType()));
                var addResp = bagFeign.add(add);
                if (!isAddSuccess(addResp)) {
                    log.warn("Return old equip to bag failed role={}, item={}", req.getRoleId(), oldItemId);
                }
            }, virtualExecutor);
        }

        CompletableFuture<Void> updateStatsFuture = CompletableFuture.runAsync(() -> {
            applyRoleStatDelta(roleIdLong,
                slot.getHp() - oldHp,
                slot.getAttack() - oldAtk,
                slot.getDefend() - oldDef,
                slot.getSpeed() - oldSpd);
        }, virtualExecutor);

        // Wait for both operations to complete
        CompletableFuture.allOf(returnOldFuture, updateStatsFuture).join();

        return EquipDTOs.OkResp.OK();
    }

    @Transactional
    public EquipDTOs.OkResp unequip(EquipDTOs.UnequipReq req) {
        var slotOpt = slotRepo.findByRoleIdAndEquipType(Long.parseLong(req.getRoleId()), req.getEquipType());
        if (slotOpt.isEmpty() || slotOpt.get().getItemId() <= 0) {
            return EquipDTOs.OkResp.NG("SLOT_EMPTY");
        }
        var slot = slotOpt.get();

        // 1) thêm item về túi
        var add = createAddItemReq(req.getRoleId(), slot.getItemId(), 1, false, "unequip", byteToInteger(req.getBagType()));
        var addResp = bagFeign.add(add);
        if (!isAddSuccess(addResp)) return EquipDTOs.OkResp.NG("BAG_ADD_FAILED");

        // 2) clear slot
        int oldHp = slot.getHp();
        int oldAtk = slot.getAttack();
        int oldDef = slot.getDefend();
        int oldSpd = slot.getSpeed();
        clearSlot(slot);
        slotRepo.save(slot);
        syncSnapshot(Long.parseLong(req.getRoleId()), slot);
        applyRoleStatDelta(Long.parseLong(req.getRoleId()), -oldHp, -oldAtk, -oldDef, -oldSpd);

        return EquipDTOs.OkResp.OK();
    }

    // NEW: wrapper wear-by-itemId (được gọi từ /wear/{roleId}/{itemId})
    @Transactional
    public EquipDTOs.OkResp wear(Long roleId, int itemId, Integer bagType) {
        var req = new EquipDTOs.EquipReq();
        req.setRoleId(String.valueOf(roleId));
        req.setItemId(itemId);
        if (bagType != null) req.setBagType(bagType.byteValue()); // nếu null -> service dùng props/equip bag type
        return equip(req);
    }


    // ================= INTERNAL (khớp /internal/equip) =================

    /**
     * Mặc món pending từ Box:
     * - KHÔNG đụng tới túi.
     * - Ghi món mới vào slot.
     * - Trả về món cũ typed để BoxService lưu compare-state/pending legacy trong giai đoạn chuyển tiếp.
     */
    @Transactional
    public EquipDTOs.WearFromBoxResp wearFromBox(EquipDTOs.WearFromBoxReq req) {
        if (req == null || req.getItem() == null) {
            throw new IllegalArgumentException("MISSING_ITEM");
        }
        String roleId = req.getRoleId();
        if (roleId == null || roleId.isBlank()) {
            throw new IllegalArgumentException("MISSING_ROLE_ID");
        }
        Long roleIdLong = Long.parseLong(roleId);
        EquipDTOs.WearFromBoxItem item = req.getItem();
        int itemId = item != null && item.getItemId() != null ? item.getItemId() : 0;
        if (itemId <= 0) {
            throw new IllegalArgumentException("BAD_ITEM");
        }

        // Xác định equipType & meta
        var metaNew = getEquipMeta(itemId);
        int reqEquipType = item != null && item.getEquipType() != null ? item.getEquipType() : -1;
        int equipType = (reqEquipType >= 0) ? reqEquipType : extractEquipType(metaNew);
        if (equipType < 0) throw new IllegalArgumentException("ITEM_NOT_EQUIPPABLE");

        // slot hiện tại
        var slot = slotRepo.findByRoleIdAndEquipType(roleIdLong, equipType)
                .orElseGet(() -> {
                    var s = new EquipSlotEntity();
                    s.setRoleId(roleIdLong);
                    s.setEquipType(equipType);
                    s.setItemId(0);
                    return s;
                });

        int oldHp = slot.getHp();
        int oldAtk = slot.getAttack();
        int oldDef = slot.getDefend();
        int oldSpd = slot.getSpeed();
        int oldAttrType1 = slot.getAttrType1();
        int oldAttrValue1 = slot.getAttrValue1();
        int oldAttrType2 = slot.getAttrType2();
        int oldAttrValue2 = slot.getAttrValue2();
        int oldItemId = slot.getItemId();
        int oldEquipType = slot.getEquipType();

        // Build "replaced" (nếu có): luôn giữ snapshot item cũ để Box flow không bị mất đồ
        // khi item-service/meta tạm thời lỗi.
        EquipDTOs.ReplacedEquip replaced = null;
        if (oldItemId > 0) {
            var metaOld = getEquipMeta(oldItemId);
            int oldLevel = extractLevel(metaOld, 1);
            int oldQuality = extractQuality(metaOld, 1);
            if (metaOld.isEmpty()) {
                log.warn("old equip meta missing itemId={} roleId={} equipType={}, fallback replaced snapshot", oldItemId, roleIdLong, oldEquipType);
            }
            replaced = EquipDTOs.ReplacedEquip.builder()
                    .itemId(oldItemId)
                    .equipType(oldEquipType)
                    .quality(oldQuality)
                    .equipLevel(oldLevel)
                    .hp(oldHp)
                    .attack(oldAtk)
                    .defend(oldDef)
                    .speed(oldSpd)
                    .attrType1(oldAttrType1)
                    .attrValue1(oldAttrValue1)
                    .attrType2(oldAttrType2)
                    .attrValue2(oldAttrValue2)
                    .build();
        }

        // Ghi món mới vào slot: ưu tiên stats đã rolled từ Box pending, fallback qua item meta.
        snapshotStatsFromWearItemOrMeta(slot, item, metaNew);
        slot.setItemId(itemId);
        slotRepo.save(slot);
        syncSnapshot(roleIdLong, slot);
        applyRoleStatDelta(roleIdLong,
            slot.getHp() - oldHp,
            slot.getAttack() - oldAtk,
            slot.getDefend() - oldDef,
            slot.getSpeed() - oldSpd);

        return EquipDTOs.WearFromBoxResp.builder().replaced(replaced).build();
    }

    /**
     * Tính coin/exp khi bán equip.
     * - Đọc meta để lấy các khóa như: sell_price / sell_exp (nếu có).
     * - Nếu request có "businessmanPermyriad" (0..10000) -> áp dụng vào coin.
     * - Nếu meta thiếu, dùng công thức fallback mềm theo quality/level.
     * - Cache kết quả dựa trên itemId, quality, level, businessman để tăng tốc độ
     */
    @Cacheable(value = "equipSellPrice", key = "#req.get('item')?.get('itemId') + '_' + #req.get('item')?.get('quality') + '_' + #req.get('item')?.get('equipLevel') + '_' + #req.get('businessmanPermyriad')", unless = "#result == null")
    public Map<String, Object> computeSell(Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        Map<String,Object> item = (Map<String,Object>) req.get("item");
        if (item == null || item.isEmpty()) {
            return Map.of("coin", 0L, "exp", 0L);
        }
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
        if (item == null || item.isEmpty()) {
            return Map.of("itemId", 0, "num", 0L, "exp", 0L);
        }
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
        if (req == null || req.isEmpty()) {
            return Map.of("equip", false);
        }
        int itemId = asInt(req.get("itemId"), 0);
        var meta = getEquipMeta(itemId);
        boolean equip = extractEquipType(meta) >= 0;
        return Map.of("equip", equip);
    }

    /** Cố gắng resolve itemId theo (equipType, quality, level). Nếu không thể, trả rỗng (để BoxService fallback). */
    public Map<String, Object> resolveItemId(Map<String, Object> req) {
        int equipType = asInt(req.get("equipType"), 0);
        int quality   = asInt(req.get("quality"), 1);
        int level     = asInt(firstNonNull(req.get("level"), req.get("equipLevel")), 1);

        int bestId = 0;
        int bestScore = Integer.MIN_VALUE;
        for (var row : equipmentConfigCache.allRows()) {
            if (row == null || row.id <= 0 || row.part != equipType) continue;

            int score = 0;
            if (row.level != null) {
                score += (row.level == level) ? 40 : -Math.abs(row.level - level);
            }
            if (row.quality != null) {
                score += (row.quality == quality) ? 30 : -Math.abs(row.quality - quality);
            }
            // Ưu tiên entry có nhiều metadata hơn để giảm chọn nhầm.
            if (row.level != null) score += 2;
            if (row.quality != null) score += 2;

            if (score > bestScore || (score == bestScore && (bestId == 0 || row.id < bestId))) {
                bestScore = score;
                bestId = row.id;
            }
        }

        if (bestId > 0) {
            return Map.of("itemId", bestId);
        }
        return Map.of();
    }

    /** Trả meta rút gọn (equipType/quality/...) để BoxService tham chiếu. */
    public Map<String, Object> itemMeta(Map<String, Object> req) {
        if (req == null || req.isEmpty()) {
            return Map.of();
        }
        int itemId = asInt(req.get("itemId"), 0);
        var meta = getEquipMeta(itemId);
        int equipType = extractEquipType(meta);
        int quality   = extractQuality(meta, 1);
        int level     = extractLevel(meta, 1);
        Map<String,Object> out = new LinkedHashMap<>();
        if (equipType >= 0) out.put("equipType", equipType);
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
        // Prefer equipment config for equip items to avoid hard dependency on item-service availability.
        try {
            var fromConfig = equipmentConfigCache.find(itemId)
                    .map(this::toMetaFromConfig)
                    .orElse(null);
            if (fromConfig != null && !fromConfig.isEmpty()) {
                return fromConfig;
            }
        } catch (Exception e) {
            log.warn("equipment config lookup failed for itemId {}: {}", itemId, e.getMessage());
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                var map = itemMetaFeign.meta(itemId);
                if (map != null && !map.isEmpty()) {
                    return map;
                }
            } catch (Exception e) {
                if (attempt == 2) {
                    log.info("item-service meta failed for itemId {} after retry: {}", itemId, e.getMessage());
                    metaFallbackCounter.increment();
                    break;
                }
                try {
                    Thread.sleep(60L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return Map.of();
    }

    private Map<String, Object> getEquipMeta(int itemId) {
        try {
            return equipmentConfigCache.find(itemId)
                    .map(this::toMetaFromConfig)
                    .orElseGet(Map::of);
        } catch (Exception e) {
            log.warn("equipment config lookup failed for itemId {}: {}", itemId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> toMetaFromConfig(EquipmentConfigCache.EquipRow row) {
        Map<String, Object> out = new HashMap<>();
        out.put("equipType", row.part);
        if (row.quality != null) out.put("quality", row.quality);
        if (row.level != null) out.put("level", row.level);
        out.put("hp", row.hp_max);
        out.put("attack", row.att_max);
        out.put("defend", row.def_max);
        out.put("speed", row.speed_max);

        var firstBonusOpt = equipmentConfigCache.resolveColorAttr(row.frist_att);
        var firstBonus = firstBonusOpt != null ? firstBonusOpt.orElse(null) : null;
        if (firstBonus != null) {
            out.put("attrType1", firstBonus.getAttrType());
            out.put("attrValue1", firstBonus.getAttrValue());
        } else if (row.frist_att != null) {
            out.put("attrType1", row.frist_att);
        }

        var secondBonusOpt = equipmentConfigCache.resolveColorAttr(row.second_att);
        var secondBonus = secondBonusOpt != null ? secondBonusOpt.orElse(null) : null;
        if (secondBonus != null) {
            out.put("attrType2", secondBonus.getAttrType());
            out.put("attrValue2", secondBonus.getAttrValue());
        } else if (row.second_att != null) {
            out.put("attrType2", row.second_att);
        }
        return out;
    }

    private boolean isConsumeSuccess(ResponseEntity<Void> response) {
        return response != null
                && (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.NO_CONTENT);
    }

    private boolean isAddSuccess(ResponseEntity<List<BagDTOs.ItemView>> response) {
        return response != null && response.getStatusCode().is2xxSuccessful();
    }

    private int extractEquipType(Map<String,Object> meta) {
        if (meta == null) return -1;
        if (!hasAny(meta, "equipType", "equip_type", "position", "pos")) return -1;
        Object v = firstNonNull(meta.get("equipType"), meta.get("equip_type"), meta.get("position"), meta.get("pos"));
        return asInt(v, -1);
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

    private void snapshotStatsFromWearItemOrMeta(EquipSlotEntity s, EquipDTOs.WearFromBoxItem item, Map<String,Object> meta) {
        Integer hp = item != null ? item.getHp() : null;
        Integer attack = item != null ? item.getAttack() : null;
        Integer defend = item != null ? item.getDefend() : null;
        Integer speed = item != null ? item.getSpeed() : null;
        Integer attrType1 = item != null ? item.getAttrType1() : null;
        Integer attrValue1 = item != null ? item.getAttrValue1() : null;
        Integer attrType2 = item != null ? item.getAttrType2() : null;
        Integer attrValue2 = item != null ? item.getAttrValue2() : null;

        s.setHp(hp != null ? hp : intVal(meta, "hp"));
        s.setAttack(attack != null ? attack : intVal(meta, "attack", "att"));
        s.setDefend(defend != null ? defend : intVal(meta, "defend", "def"));
        s.setSpeed(speed != null ? speed : intVal(meta, "speed", "spd"));
        s.setAttrType1(attrType1 != null ? attrType1 : intVal(meta, "attrType1", "attr_type1", "fristAtt"));
        s.setAttrValue1(attrValue1 != null ? attrValue1 : intVal(meta, "attrValue1", "attr_value1", "fristAttValue"));
        s.setAttrType2(attrType2 != null ? attrType2 : intVal(meta, "attrType2", "attr_type2", "secondAtt"));
        s.setAttrValue2(attrValue2 != null ? attrValue2 : intVal(meta, "attrValue2", "attr_value2", "secondAttValue"));
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
        if (m == null) return false;
        for (String k: ks) if (m.containsKey(k)) return true; return false;
    }
    private static int intVal(Map<String,Object> m, String... keys) {
        if (m == null || keys == null) return 0;
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

    /**
     * Helper to create BagConsumeReq with simplified params
     */
    private BagConsumeReq createConsumeReq(String roleId, int itemId, int amount, String source) {
        return BagConsumeReq.builder()
                .userId(1L) // audit field - roleId used for bag operations
                .roleId(Long.parseLong(roleId))
                .itemId(itemId)
                .amount(amount)
                .source(source)
                .build();
    }

    /**
     * Helper to create BagAddItemReq  with simplified params
     */
    private BagAddItemReq createAddItemReq(String roleId, int itemId, int amount, boolean bound, String source, Integer bagTypeOverride) {
        Map<String, Object> meta = getOneMeta(itemId);
        var item = BagAddItemReq.Item.builder()
                .itemId(itemId)
                .amount(amount)
                .quality(extractQuality(meta, 1))
                .bagType(resolveBagType(bagTypeOverride))
                .bound(bound)
                .build();
        return BagAddItemReq.builder()
                .userId(1L) // audit field - roleId used for bag operations
                .roleId(Long.parseLong(roleId))
                .items(List.of(item))
                .source(source)
                .build();
    }

    private int resolveBagType(Integer bagTypeOverride) {
        if (bagTypeOverride != null) return Math.max(0, bagTypeOverride);
        return Math.max(0, props.getEquipBagType());
    }

    private Integer byteToInteger(Byte value) {
        return value == null ? null : (int) value;
    }

    private void applyRoleStatDelta(Long roleId, int hpDelta, int attackDelta, int defenseDelta, int speedDelta) {
        if (hpDelta == 0 && attackDelta == 0 && defenseDelta == 0 && speedDelta == 0) {
            return;
        }
        try {
            roleFeign.applyStatDelta(roleId,
                    new RoleFeign.StatDeltaReq(hpDelta, attackDelta, defenseDelta, speedDelta));
        } catch (Exception e) {
            log.warn("Apply role stat delta failed roleId={}, hp={}, atk={}, def={}, spd={}: {}",
                    roleId, hpDelta, attackDelta, defenseDelta, speedDelta, e.getMessage());
        }
    }
    public EquipDTOs.OkResp bagSell(Long roleId, int equipType) {
        var req = new EquipDTOs.UnequipReq();
        req.setRoleId(String.valueOf(roleId));
        req.setEquipType(equipType);
        return unequip(req);
    }
    @Transactional
    public EquipDTOs.OkResp transform(Long roleId, int equipType, int targetRank) {
        var slotOpt = slotRepo.findByRoleIdAndEquipType(roleId, equipType);
        if (slotOpt.isEmpty() || slotOpt.get().getItemId() <= 0) {
            return EquipDTOs.OkResp.NG("SLOT_EMPTY");
        }
        var slot   = slotOpt.get();
        int bonus  = Math.max(1, targetRank);
        int oldHp  = slot.getHp();
        int oldAtk = slot.getAttack();
        slot.setHp(slot.getHp() + bonus);
        slot.setAttack(slot.getAttack() + bonus);
        slotRepo.save(slot);
        syncSnapshot(roleId, slot);
        applyRoleStatDelta(roleId, slot.getHp() - oldHp, slot.getAttack() - oldAtk, 0, 0);
        return EquipDTOs.OkResp.OK();
    }

    private void syncSnapshot(Long roleId, EquipSlotEntity slot) {
        if (roleId == null || slot == null) {
            return;
        }
        int equipType = slot.getEquipType();
        if (slot.getItemId() > 0) {
            snapshotRepo.save(roleId, equipType, toEquipItem(slot));
        } else {
            snapshotRepo.delete(roleId, equipType);
        }
    }
}