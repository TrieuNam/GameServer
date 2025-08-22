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

    public EquipDTOs.ListResp list(String roleId) {
        var list = slotRepo.findByRoleId(roleId);
        var items = new ArrayList<EquipDTOs.EquipItem>();
        for (var e : list) {
            items.add(toEquipItem(e));
        }
        return new EquipDTOs.ListResp(items);
    }

    @Transactional
    public EquipDTOs.OkResp equip(EquipDTOs.EquipReq req) {
        // 1) Lấy meta item để biết equipType + stat
        var meta = getOneMeta(req.getItemId());
        int equipType = extractEquipType(meta);
        if (equipType <= 0) {
            return EquipDTOs.OkResp.NG("ITEM_NOT_EQUIPPABLE");
        }

        // 2) Trừ item trong túi
        var consume = new BagDTOs.ConsumeReq(
                req.getRoleId(),
                req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                List.of(new BagDTOs.ItemDelta(req.getItemId(), 1, false, "equip")),
                1600, 1
        );
        var ok = bagFeign.consume(consume);
        if (ok == null || !ok.ok()) {
            return EquipDTOs.OkResp.NG("ITEM_NOT_ENOUGH");
        }

        // 3) Nếu slot đã có item -> trả lại túi
        var slot = slotRepo.findByRoleIdAndEquipType(req.getRoleId(), equipType)
                .orElseGet(() -> {
                    var s = new EquipSlotEntity();
                    s.setRoleId(req.getRoleId());
                    s.setEquipType(equipType);
                    s.setItemId(0);
                    return s;
                });

        if (slot.getItemId() > 0) {
            var add = new BagDTOs.AddItemReq(
                    req.getRoleId(),
                    req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                    List.of(new BagDTOs.ItemDelta(slot.getItemId(), 1, false, "unequip")),
                    1600, 2
            );
            var addResp = bagFeign.add(add);
            if (addResp == null || addResp.added() == null) {
                log.warn("Return old equip to bag failed role={}, item={}", req.getRoleId(), slot.getItemId());
            }
        }

        // 4) Lưu slot mới + snapshot stats (nếu cần)
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

        // 1) Thêm item về túi
        var add = new BagDTOs.AddItemReq(
                req.getRoleId(),
                req.getBagType() == null ? props.getEquipBagType() : req.getBagType(),
                List.of(new BagDTOs.ItemDelta(slot.getItemId(), 1, false, "unequip")),
                1600, 3
        );
        var addResp = bagFeign.add(add);
        if (addResp == null || addResp.added() == null) {
            return EquipDTOs.OkResp.NG("BAG_ADD_FAILED");
        }

        // 2) Clear slot
        slot.setItemId(0);
        slot.setHp(0); slot.setAttack(0); slot.setDefend(0); slot.setSpeed(0);
        slot.setAttrType1(0); slot.setAttrValue1(0);
        slot.setAttrType2(0); slot.setAttrValue2(0);
        slotRepo.save(slot);

        return EquipDTOs.OkResp.OK();
    }

    // ===== helpers

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

    private Map<String,Object> getOneMeta(int itemId) {
        var map = itemMetaFeign.batchMeta(String.valueOf(itemId));
        return map == null ? Map.of() : map.getOrDefault(String.valueOf(itemId), Map.of());
    }

    /** Cố gắng bắt các key phổ biến từ meta */
    private int extractEquipType(Map<String,Object> meta) {
        if (meta == null) return 0;
        Object v = firstNonNull(meta.get("equipType"), meta.get("equip_type"), meta.get("position"), meta.get("pos"));
        if (v instanceof Number n) return n.intValue();
        try {
            return v == null ? 0 : Integer.parseInt(v.toString());
        } catch (Exception ignore) { return 0; }
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

    private static Object firstNonNull(Object... xs) {
        for (Object x : xs) if (x != null) return x;
        return null;
    }
    private static int intVal(Map<String,Object> m, String... keys) {
        for (var k : keys) {
            Object v = m.get(k);
            if (v instanceof Number n) return n.intValue();
            if (v != null) try { return Integer.parseInt(v.toString()); } catch (Exception ignore){}
        }
        return 0;
    }
}