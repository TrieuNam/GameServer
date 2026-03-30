package com.SouthMillion.box_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxEquipService implements BoxInfoServiceImpl {

    private final BoxService svc;
    /**
     * Lấy EquipInfo (đơn trị) từ trạng thái hiện tại.
     * Ưu tiên bóc từ "pending" của /info; nếu không có thì trả về object rỗng (isNew=0).
     */
    public BoxDTOs.EquipInfo getEquipInfo(Long roleId) {
        BoxDTOs.InfoResp info = null;
        try {
            info = svc.info(roleId);
        } catch (Exception e) {
            log.warn("[box] getEquipInfo: getInfo error rid={} ex={}", roleId, e.toString());
        }

        BoxDTOs.BoxCompareStateResp compareState = info != null ? info.getCompareState() : null;
        BoxDTOs.EquipInfo equipInfo = BoxDTOs.equipInfoFromCompareState(compareState, 0);
        if (equipInfo == null) {
            Map<String, Object> pending = (info != null) ? info.getPending() : null;
            Integer isNew = extractIsNew(pending);
            if (isNew == null) isNew = 0;
            equipInfo = BoxDTOs.equipInfoFromPending(pending, isNew);
        }
        if (equipInfo != null) {
            return equipInfo;
        }

        // fallback: trả object rỗng (để client vẫn emit được SC_BOX_EQUIP_INFO với 0-values)
        return BoxDTOs.EquipInfo.builder()
            .isNew(0)
                .equipInfo(BoxDTOs.EquipRolled.builder().build())
                .build();
    }

    /** cố gắng lấy isNew từ pending hoặc pending.equip */
    @SuppressWarnings("unchecked")
    private Integer extractIsNew(Map<String, Object> pending) {
        if (pending == null || pending.isEmpty()) return null;
        Integer v = pickInt(pending, "isNew", "is_new", "equipIsNew", "equip_is_new");
        if (v != null) return v;

        Object nested = pending.get("equip");
        if (nested instanceof Map<?, ?> m) {
            return pickInt((Map<String, Object>) m, "isNew", "is_new", "equipIsNew", "equip_is_new");
        }
        return null;
    }

    private Integer pickInt(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String k : keys) {
            Object v = map.get(k);
            Integer got = toInt(v);
            if (got != null) return got;
        }
        return null;
    }

    private Integer toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try {
            if (v instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        } catch (Exception ignore) {}
        return null;
    }
}