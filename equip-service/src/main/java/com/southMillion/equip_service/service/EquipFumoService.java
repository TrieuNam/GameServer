package com.SouthMillion.equip_service.service;

import com.SouthMillion.equip_service.config.EquipProperties;
import com.SouthMillion.equip_service.entity.EquipFumoEntity;
import com.SouthMillion.equip_service.repository.EquipFumoRepository;
import com.SouthMillion.equip_service.service.client.BagInternalFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagConsumeReq;
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

    public EquipFumoDTOs.FumoListResp list(Long roleId) {
        var es = repo.findByRoleId(roleId);
        var out = new ArrayList<EquipFumoDTOs.FumoData>(es.size());
        for (var e : es) out.add(toDto(e));
        return new EquipFumoDTOs.FumoListResp(out);
    }

    public EquipFumoDTOs.FumoOneResp one(Long roleId, int equipType) {
        var e = repo.findByRoleIdAndEquipType(roleId, equipType).orElse(null);
        return new EquipFumoDTOs.FumoOneResp(equipType, toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.FumoOneResp addExp(EquipFumoDTOs.AddExpReq req) {
        if (req.costItems() != null && !req.costItems().isEmpty()) {
            var consume = createBatchConsumeReq(req.roleId(), req.costItems(), "fumo_add_exp");
            var resp = bagFeign.consume(consume);
            if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
                return new EquipFumoDTOs.FumoOneResp(req.equipType(), null);
            }
        }

        Long roleIdLong = Long.parseLong(req.roleId());
        var e = repo.findByRoleIdAndEquipType(roleIdLong, req.equipType())
                .orElseGet(() -> EquipFumoEntity.builder()
                        .roleId(roleIdLong)
                        .equipType(req.equipType())
                        .level(0).exp(0).endTime(0)
                        .build());

        int level = e.getLevel();
        int exp   = e.getExp() + req.addExp();

        int maxLv = props.getFumoMaxLevel();
        while (level < maxLv) {
            int need = expNeedForLevel(level + 1);
            if (exp >= need) { exp -= need; level++; } else break;
        }
        e.setLevel(level);
        e.setExp(Math.max(exp, 0));
        repo.save(e);

        return new EquipFumoDTOs.FumoOneResp(req.equipType(), toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.FumoOneResp activate(EquipFumoDTOs.ActivateReq req) {
        Long roleIdLong = Long.parseLong(req.roleId());
        var e = repo.findByRoleIdAndEquipType(roleIdLong, req.equipType())
                .orElseGet(() -> EquipFumoEntity.builder()
                        .roleId(roleIdLong)
                        .equipType(req.equipType())
                        .level(0).exp(0).endTime(0)
                        .build());
        e.setEndTime(req.endTimeEpochSec());
        repo.save(e);
        return new EquipFumoDTOs.FumoOneResp(req.equipType(), toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.OkResp reset(EquipFumoDTOs.ResetReq req) {
        consumeIfNeeded(req.roleId(), req.costItems(), "fumo_reset");
        Long roleIdLong = Long.parseLong(req.roleId());
        var e = repo.findByRoleIdAndEquipType(roleIdLong, req.equipType()).orElse(null);
        if (e == null) return EquipFumoDTOs.OkResp.OK();
        e.setLevel(0); e.setExp(0); e.setEndTime(0);
        repo.save(e);
        return EquipFumoDTOs.OkResp.OK();
    }

    // helpers
    private void consumeIfNeeded(String roleId, Map<Integer,Long> items, String reason) {
        if (items == null || items.isEmpty()) return;
        var consume = createBatchConsumeReq(roleId, items, reason);
        var resp = bagFeign.consume(consume);
        if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("COST_ITEM_NOT_ENOUGH");
        }
    }

    private int expNeedForLevel(int targetLevel) {
        int base = props.getFumoBaseExp();
        int grow = props.getFumoGrowExp();
        long val = (long) base + (long) grow * (targetLevel - 1L);
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, val));
    }

    private EquipFumoDTOs.FumoData toDto(EquipFumoEntity e) {
        if (e == null) return null;
        return new EquipFumoDTOs.FumoData(e.getLevel(), e.getExp(), e.getEndTime());
    }

    /**
     * Helper to consume items from bag - handles batch consume
     */
    private BagConsumeReq createBatchConsumeReq(String roleId, Map<Integer, Long> items, String source) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        var costs = items.entrySet().stream()
                .map(e -> new BagConsumeReq.Cost(e.getKey(), e.getValue().intValue()))
                .toList();
        return BagConsumeReq.builder()
                .userId(1L) // audit field
                .roleId(Long.parseLong(roleId))
                .itemId(0) // Not used for batch
                .amount(0) // Not used for batch
                .costs(costs)
                .source(source)
                .build();
    }
}