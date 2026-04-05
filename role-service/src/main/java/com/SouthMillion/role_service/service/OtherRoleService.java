package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.service.client.EquipFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtherRoleService {

    private final RoleRepository roleRepo;
    private final EquipFeign equipFeign;

    @Transactional(readOnly = true)
    public OtherRoleDTOs.OtherRoleInfo getOtherRole(String uid, String roleIdOpt) {
        Role r = (roleIdOpt != null && !roleIdOpt.isBlank())
                ? roleRepo.findById(Long.valueOf(roleIdOpt)).orElseThrow(() -> new IllegalArgumentException("Role not found"))
                : roleRepo.findFirstByUserIdOrderByCreatedAtAsc(uid).orElseThrow(() -> new IllegalArgumentException("Role not found for uid"));

        var attr = new OtherRoleDTOs.OtherRoleAttr(r.getLevel(), r.getExp(), r.getHp(), r.getAttackValue(), r.getDefenseValue(), r.getSpeed());
        Map<Integer, Long> totals = new LinkedHashMap<>();
        totals.put(1, Math.max(0L, r.getHp()));
        totals.put(2, Math.max(0L, r.getAttackValue()));
        totals.put(3, Math.max(0L, r.getDefenseValue()));
        totals.put(4, Math.max(0L, r.getSpeed()));

        // Fetch equip list once — use it for both attr aggregation and equip-slot list.
        List<String> equipmentSlots = Collections.emptyList();
        try {
            EquipDTOs.ListResp equipped = equipFeign.list(r.getRoleId());
            if (equipped != null && equipped.getItems() != null) {
                List<String> slots = new ArrayList<>();
                for (EquipDTOs.EquipItem item : equipped.getItems()) {
                    if (item == null) continue;
                    mergeAttr(totals, item.getAttrType1(), item.getAttrValue1());
                    mergeAttr(totals, item.getAttrType2(), item.getAttrValue2());
                    // Encode as "equipType:itemId" for the websocket layer to decode into PB_EquipData.
                    slots.add(item.getEquipType() + ":" + item.getItemId());
                }
                equipmentSlots = slots;
            }
        } catch (Exception e) {
            log.debug("[other-role] equip attr aggregation skipped roleId={} ex={}", r.getRoleId(), e.toString());
        }

        return new OtherRoleDTOs.OtherRoleInfo(
                r.getUserId(),
                String.valueOf(r.getRoleId()),
                r.getName(),
                r.getHeadChar(),
                r.getGuildName(),
                attr,
                toAttrPairs(totals),
                equipmentSlots,
                Collections.emptyList(),
                computeCapability(totals),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private void mergeAttr(Map<Integer, Long> totals, Integer attrType, Integer attrValue) {
        if (attrType == null || attrType <= 0 || attrValue == null || attrValue == 0) {
            return;
        }
        totals.merge(attrType, (long) attrValue, Long::sum);
    }

    private List<OtherRoleDTOs.OtherRoleAttrPair> toAttrPairs(Map<Integer, Long> totals) {
        List<OtherRoleDTOs.OtherRoleAttrPair> pairs = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : totals.entrySet()) {
            if (entry.getKey() == null || entry.getKey() <= 0) {
                continue;
            }
            pairs.add(new OtherRoleDTOs.OtherRoleAttrPair(entry.getKey(), Math.max(0L, entry.getValue())));
        }
        return pairs;
    }

    private long computeCapability(Map<Integer, Long> totals) {
        long hp = Math.max(0L, totals.getOrDefault(1, 0L));
        long atk = Math.max(0L, totals.getOrDefault(2, 0L));
        long def = Math.max(0L, totals.getOrDefault(3, 0L));
        long spd = Math.max(0L, totals.getOrDefault(4, 0L));
        long extra = 0L;
        for (Map.Entry<Integer, Long> entry : totals.entrySet()) {
            if (entry.getKey() != null && entry.getKey() > 4) {
                extra += Math.max(0L, entry.getValue());
            }
        }
        return hp + atk + def + (spd * 10L) + extra;
    }
}