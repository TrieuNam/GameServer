package com.southMillion.equip_service.service;

import com.southMillion.equip_service.config.EquipProperties;
import com.southMillion.equip_service.entity.EquipFumoEntity;
import com.southMillion.equip_service.repository.EquipFumoRepository;
import com.southMillion.equip_service.service.client.BagInternalFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipFumoService {

    private final EquipFumoRepository repo;
    private final BagInternalFeign bagFeign;
    private final EquipProperties props;

    public EquipFumoDTOs.FumoListResp list(String roleId) {
        var es = repo.findByRoleId(roleId);
        var out = new ArrayList<EquipFumoDTOs.FumoData>(es.size());
        for (var e : es) out.add(toDto(e));
        return new EquipFumoDTOs.FumoListResp(out);
    }

    public EquipFumoDTOs.FumoOneResp one(String roleId, int equipType) {
        var e = repo.findByRoleIdAndEquipType(roleId, equipType).orElse(null);
        return new EquipFumoDTOs.FumoOneResp(equipType, toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.FumoOneResp addExp(EquipFumoDTOs.AddExpReq req) {
        // 1) consume vật phẩm (nếu có)
        if (req.costItems() != null && !req.costItems().isEmpty()) {
            var deltas = req.costItems().entrySet().stream()
                    .map(e -> new BagDTOs.ItemDelta(e.getKey(), e.getValue(), false, "fumo_add_exp"))
                    .toList();
            var consume = new BagDTOs.ConsumeReq(req.roleId(), (byte)0, deltas, 1603, 1);
            var ok = bagFeign.consume(consume);
            if (ok == null || !ok.ok()) {
                return new EquipFumoDTOs.FumoOneResp(req.equipType(),
                        null /* failed -> không thay đổi */);
            }
        }

        // 2) upsert & tăng exp
        var e = repo.findByRoleIdAndEquipType(req.roleId(), req.equipType())
                .orElseGet(() -> EquipFumoEntity.builder()
                        .roleId(req.roleId())
                        .equipType(req.equipType())
                        .level(0).exp(0).endTime(0)
                        .build());

        int level = e.getLevel();
        int exp   = e.getExp() + req.addExp();

        int maxLv = props.getFumoMaxLevel();
        while (level < maxLv) {
            int need = expNeedForLevel(level + 1);
            if (exp >= need) {
                exp -= need;
                level++;
            } else break;
        }
        e.setLevel(level);
        e.setExp(Math.max(exp, 0));
        repo.save(e);

        return new EquipFumoDTOs.FumoOneResp(req.equipType(), toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.FumoOneResp activate(EquipFumoDTOs.ActivateReq req) {
        var e = repo.findByRoleIdAndEquipType(req.roleId(), req.equipType())
                .orElseGet(() -> EquipFumoEntity.builder()
                        .roleId(req.roleId())
                        .equipType(req.equipType())
                        .level(0).exp(0).endTime(0)
                        .build());
        e.setEndTime(req.endTimeEpochSec());
        repo.save(e);
        return new EquipFumoDTOs.FumoOneResp(req.equipType(), toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.OkResp reset(EquipFumoDTOs.ResetReq req) {
        // Optional: consume cost items before reset
        consumeIfNeeded(req.roleId(), req.costItems(), "fumo_reset");
        var e = repo.findByRoleIdAndEquipType(req.roleId(), req.equipType()).orElse(null);
        if (e == null) return EquipFumoDTOs.OkResp.OK();
        e.setLevel(0);
        e.setExp(0);
        e.setEndTime(0);
        repo.save(e);
        return EquipFumoDTOs.OkResp.OK();
    }

    // ===== helpers

    private void consumeIfNeeded(String roleId, Map<Integer,Long> items, String reason) {
        if (items == null || items.isEmpty()) return;
        var deltas = items.entrySet().stream()
                .map(e -> new BagDTOs.ItemDelta(e.getKey(), e.getValue(), false, reason))
                .toList();
        var ok = bagFeign.consume(new BagDTOs.ConsumeReq(roleId, (byte)0, deltas, 1603, 2));
        if (ok == null || !ok.ok()) throw new IllegalStateException("COST_ITEM_NOT_ENOUGH");
    }

    private int expNeedForLevel(int targetLevel) {
        // công thức đơn giản: need = base + grow*(L-1)
        int base = props.getFumoBaseExp();
        int grow = props.getFumoGrowExp();
        long val = (long) base + (long) grow * (targetLevel - 1L);
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, val));
    }

    private EquipFumoDTOs.FumoData toDto(EquipFumoEntity e) {
        if (e == null) return null;
        return new EquipFumoDTOs.FumoData(e.getLevel(), e.getExp(), e.getEndTime());
    }
}