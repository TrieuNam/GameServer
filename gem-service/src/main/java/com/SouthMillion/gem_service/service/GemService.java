package com.SouthMillion.gem_service.service;
import com.SouthMillion.gem_service.entity.GemData;
import com.SouthMillion.gem_service.repository.GemDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Slf4j @Service @RequiredArgsConstructor
public class GemService {
    private final GemDataRepository repository;

    public Map<String, Object> getGemInfo(Long roleId) {
        List<GemData> gems = repository.findByRoleId(roleId);
        return Map.of("drawingId", -1, "gems", gems);
    }

    @Transactional
    public Map<String, Object> inlay(Long roleId, Integer gemId, Integer slotType) {
        Optional<GemData> opt = repository.findByRoleIdAndGemId(roleId, gemId);
        if (opt.isEmpty()) return Map.of("success", false, "reason", "gem not found");
        GemData gem = opt.get();
        gem.setIsInlaid(true);
        gem.setSlotType(slotType != null ? slotType : 0);
        repository.save(gem);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> remove(Long roleId, Integer gemId) {
        Optional<GemData> opt = repository.findByRoleIdAndGemId(roleId, gemId);
        if (opt.isEmpty()) return Map.of("success", false);
        GemData gem = opt.get();
        gem.setIsInlaid(false);
        gem.setSlotType(-1);
        repository.save(gem);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> compose(Long roleId, List<Integer> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Map.of("success", false, "reason", "No source gems provided");
        }
        // Consume source gems (reduce count of each by 1)
        for (Integer srcId : sourceIds) {
            repository.findByRoleIdAndGemId(roleId, srcId).ifPresent(g -> {
                g.setCount(Math.max(0, g.getCount() - 1));
                repository.save(g);
            });
        }
        // Produce composed gem: upgrade the base gem's level and increment its count
        int baseGemId = sourceIds.get(0);
        Optional<GemData> baseOpt = repository.findByRoleIdAndGemId(roleId, baseGemId);
        int newLevel = baseOpt.map(g -> g.getLevel() + 1).orElse(2);
        GemData composed = baseOpt.orElseGet(() -> GemData.builder()
                .roleId(roleId).gemId(baseGemId).level(1).count(0)
                .isInlaid(false).slotType(-1).build());
        composed.setLevel(newLevel);
        composed.setCount(composed.getCount() + 1);
        repository.save(composed);
        log.info("Gem composed: roleId={}, gemId={}, newLevel={}", roleId, baseGemId, newLevel);
        return Map.of("success", true, "composedGemId", baseGemId, "newLevel", newLevel);
    }

    @Transactional
    public Map<String, Object> upgradeAll(Long roleId, List<Integer> itemIds) {
        for (Integer itemId : itemIds) {
            repository.findByRoleIdAndGemId(roleId, itemId).ifPresent(g -> {
                g.setLevel(g.getLevel() + 1);
                repository.save(g);
            });
        }
        return getGemInfo(roleId);
    }

    @Transactional
    public Map<String, Object> buy(Long roleId, List<Integer> itemIds) {
        for (Integer itemId : itemIds) {
            GemData existing = repository.findByRoleIdAndGemId(roleId, itemId)
                    .orElseGet(() -> GemData.builder()
                            .roleId(roleId).gemId(itemId).level(1).count(0)
                            .isInlaid(false).slotType(-1).build());
            existing.setCount(existing.getCount() + 1);
            repository.save(existing);
        }
        return getGemInfo(roleId);
    }
}
