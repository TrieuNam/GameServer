package com.SouthMillion.equip_service.service;

import com.SouthMillion.equip_service.config.EquipProperties;
import com.SouthMillion.equip_service.entity.EquipFumoEntity;
import com.SouthMillion.equip_service.repository.EquipFumoRepository;
import com.SouthMillion.equip_service.service.client.BagInternalFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        int maxPart = es.stream().mapToInt(EquipFumoEntity::getEquipType).max().orElse(-1);
        int slotCount = Math.max(props.getFumoSlotCount(), maxPart + 1);
        var out = new ArrayList<EquipFumoDTOs.FumoData>(slotCount);

        Map<Integer, EquipFumoEntity> byPart = new HashMap<>();
        for (var e : es) {
            byPart.put(e.getEquipType(), e);
        }
        for (int part = 0; part < slotCount; part++) {
            out.add(toDto(byPart.get(part)));
        }
        return new EquipFumoDTOs.FumoListResp(out);
    }

    public EquipFumoDTOs.FumoOneResp one(Long roleId, int equipType) {
        var e = repo.findByRoleIdAndEquipType(roleId, equipType).orElse(null);
        return new EquipFumoDTOs.FumoOneResp(equipType, toDto(e));
    }

    @Transactional
    public EquipFumoDTOs.FumoOneResp addExp(EquipFumoDTOs.AddExpReq req) {
        Long roleIdLong = Long.parseLong(req.roleId());
        if (req.equipType() < 0) {
            return new EquipFumoDTOs.FumoOneResp(req.equipType(), null);
        }

        var e = repo.findByRoleIdAndEquipType(roleIdLong, req.equipType())
                .orElseGet(() -> EquipFumoEntity.builder()
                        .roleId(roleIdLong)
                        .equipType(req.equipType())
                        .level(0).exp(0).endTime(0)
                        .build());

        var consumeCosts = resolveAddExpCostItems(e.getLevel());
        var consume = createBatchConsumeReq(req.roleId(), consumeCosts, "fumo_add_exp");
        var consumeResp = bagFeign.consume(consume);
        if (consumeResp == null || !consumeResp.getStatusCode().is2xxSuccessful()) {
            return new EquipFumoDTOs.FumoOneResp(req.equipType(), null);
        }

        int level = e.getLevel();
        int addExp = req.addExp() > 0 ? req.addExp() : props.getFumoAddExpPerClick();
        expNeedForLevel(1); // keep fallback thresholds initialized via properties
        int exp   = e.getExp() + Math.max(addExp, 1);

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

    @Transactional
    public EquipFumoDTOs.OkResp transform(String roleId, int countFali, int countShengming, int countMohe) {
        int c1 = Math.max(0, countFali);
        int c2 = Math.max(0, countShengming);
        int c3 = Math.max(0, countMohe);
        if (c1 == 0 && c2 == 0 && c3 == 0) {
            return EquipFumoDTOs.OkResp.OK();
        }

        long powderNeedL =
                (long) c1 * Math.max(1, props.getFumoTransformFaliPowderCost()) +
                (long) c2 * Math.max(1, props.getFumoTransformShengmingPowderCost()) +
                (long) c3 * Math.max(1, props.getFumoTransformMohePowderCost());
        int powderNeed = (int) Math.min(Integer.MAX_VALUE, powderNeedL);

        var consumeReq = BagConsumeReq.builder()
                .userId(1L)
                .roleId(Long.parseLong(roleId))
                .itemId(Math.max(1, props.getFumoTransformPowderItemId()))
                .amount(powderNeed)
                .source("fumo_transform")
                .build();
        var consumeResp = bagFeign.consume(consumeReq);
        if (consumeResp == null || !consumeResp.getStatusCode().is2xxSuccessful()) {
            return EquipFumoDTOs.OkResp.NG("COST_ITEM_NOT_ENOUGH");
        }

        List<BagAddItemReq.Item> items = new ArrayList<>();
        if (c1 > 0) {
            items.add(BagAddItemReq.Item.builder()
                    .itemId(props.getFumoTransformFaliItemId())
                    .amount(c1)
                    .build());
        }
        if (c2 > 0) {
            items.add(BagAddItemReq.Item.builder()
                    .itemId(props.getFumoTransformShengmingItemId())
                    .amount(c2)
                    .build());
        }
        if (c3 > 0) {
            items.add(BagAddItemReq.Item.builder()
                    .itemId(props.getFumoTransformMoheItemId())
                    .amount(c3)
                    .build());
        }

        if (!items.isEmpty()) {
            var addReq = BagAddItemReq.builder()
                    .userId(1L)
                    .roleId(Long.parseLong(roleId))
                    .items(items)
                    .source("fumo_transform")
                    .build();
            bagFeign.add(addReq);
        }
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

    private Map<Integer, Long> resolveAddExpCostItems(int level) {
        int safeLevel = Math.max(level, 0);

        Map<Integer, Long> out = new HashMap<>();
        int threshold = Math.max(0, props.getFumoCostCoreThresholdLevel());
        if (safeLevel < threshold) {
            long powder = Math.max(1, props.getFumoCostPowderBase()) + (long) safeLevel * Math.max(0, props.getFumoCostPowderStep());
            out.put(props.getFumoCostPowderItemId(), powder);
        } else {
            int offset = safeLevel - threshold;
            long core = Math.max(1, props.getFumoCostCoreBase()) + (offset / 2L);
            out.put(props.getFumoCostCoreItemId(), core);
        }

        long secondary = Math.max(1, props.getFumoCostSecondaryBase()) + (long) safeLevel * Math.max(0, props.getFumoCostSecondaryStep());
        out.put(props.getFumoCostFaliItemId(), secondary);
        out.put(props.getFumoCostShengmingItemId(), secondary);
        return out;
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