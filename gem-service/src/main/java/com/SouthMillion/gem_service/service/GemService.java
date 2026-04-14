package com.SouthMillion.gem_service.service;
import com.SouthMillion.gem_service.entity.GemData;
import com.SouthMillion.gem_service.repository.GemDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
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

    /**
     * MOVE (op_type=4): di chuyển gem sang vị trí mới trong drawing.
     * slotType lưu encoded position = x*100 + y (giống proto PB_GemInlayData.pos).
     * gemIndex là thứ tự trong danh sách gem đang inlaid (0-5), sắp xếp theo slotType.
     */
    @Transactional
    public Map<String, Object> move(Long roleId, Integer drawingId, Integer gemIndex, Integer x, Integer y) {
        List<GemData> inlaid = repository.findByRoleIdAndIsInlaid(roleId, true).stream()
                .sorted(Comparator.comparingInt(g -> (g.getSlotType() != null ? g.getSlotType() : -1)))
                .collect(Collectors.toList());
        if (gemIndex < 0 || gemIndex >= inlaid.size()) {
            return Map.of("success", false, "reason", "gem index out of range");
        }
        GemData gem = inlaid.get(gemIndex);
        gem.setSlotType(x * 100 + y);
        repository.save(gem);
        log.info("[Gem] move: roleId={} gemId={} -> ({},{})", roleId, gem.getGemId(), x, y);
        return Map.of("success", true);
    }

    /**
     * TRANSFORM (op_type=5): chuyển đổi loại gem.
     * sourceGemId → tiêu hao, targetGemId → nhận level + count từ source.
     */
    @Transactional
    public Map<String, Object> transform(Long roleId, Integer sourceGemId, Integer targetGemId) {
        Optional<GemData> sourceOpt = repository.findByRoleIdAndGemId(roleId, sourceGemId);
        if (sourceOpt.isEmpty()) {
            return Map.of("success", false, "reason", "source gem not found");
        }
        GemData source = sourceOpt.get();
        GemData target = repository.findByRoleIdAndGemId(roleId, targetGemId)
                .orElseGet(() -> GemData.builder()
                        .roleId(roleId).gemId(targetGemId)
                        .level(source.getLevel()).count(0)
                        .isInlaid(false).slotType(-1).build());
        target.setLevel(source.getLevel());
        target.setCount(target.getCount() + source.getCount());
        repository.save(target);
        // Xoá source sau khi chuyển
        source.setCount(0);
        source.setIsInlaid(false);
        source.setSlotType(-1);
        repository.save(source);
        log.info("[Gem] transform: roleId={} {}->{}  newLevel={}", roleId, sourceGemId, targetGemId, target.getLevel());
        return Map.of("success", true, "targetGemId", targetGemId, "newLevel", target.getLevel());
    }

    /**
     * LEVEL_UP (op_type=6): nâng cấp 1 gem đơn lẻ.
     */
    @Transactional
    public Map<String, Object> levelUp(Long roleId, Integer gemId) {
        Optional<GemData> opt = repository.findByRoleIdAndGemId(roleId, gemId);
        if (opt.isEmpty()) {
            return Map.of("success", false, "reason", "gem not found");
        }
        GemData gem = opt.get();
        gem.setLevel(gem.getLevel() + 1);
        repository.save(gem);
        log.info("[Gem] levelUp: roleId={} gemId={} newLevel={}", roleId, gemId, gem.getLevel());
        return Map.of("success", true, "newLevel", gem.getLevel());
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
