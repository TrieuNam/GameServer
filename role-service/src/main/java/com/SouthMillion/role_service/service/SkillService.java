package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.SkillConfigCache;
import com.SouthMillion.role_service.entity.RoleSkill;
import com.SouthMillion.role_service.entity.RoleTalent;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.repository.RoleSkillRepository;
import com.SouthMillion.role_service.repository.RoleTalentRepository;
import com.SouthMillion.role_service.service.client.WalletFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.SouthMillion.dto.skill.SkillDTOs.SkillErrorCodes;

/**
 * Xử lý nghiệp vụ kỹ năng (skill) và thiên phú (talent) của nhân vật.
 *
 * <p>Mapping MsgId:
 * <ul>
 *   <li>1470 PB_CSRoleSkillOperaReq  → {@code getSkillAll}, {@code learnSkill}, {@code oneKeyLevelUp}</li>
 *   <li>1480 PB_CSRoleTalentOperaReq → {@code getTalentAll}, {@code learnTalent}</li>
 * </ul>
 *
 * <p>Max level per skill được đọc từ {@link SkillConfigCache} (single_skill.json / passive_skill.json).
 * Fallback: {@code skill.config.default-max-level} (default 20).
 *
 * <p>Validate skillId tồn tại trong config: {@code SkillConfigCache#isValidSkillId} /
 * {@code SkillConfigCache#isValidTalentId}.
 *
 * <p>TODO: tích hợp WalletService kiểm tra & trừ kim tiền khi học/nâng cấp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final RoleRepository roleRepo;
    private final RoleSkillRepository skillRepo;
    private final RoleTalentRepository talentRepo;
    private final SkillConfigCache skillConfigCache;
    private final WalletFeign walletFeign;
    private final ConcurrentMap<Long, Object> roleLocks = new ConcurrentHashMap<>();

    @Value("${skill.economy.enabled:true}")
    private boolean economyEnabled = true;

    @Value("${skill.economy.currency-item-id:1}")
    private long economyCurrencyItemId = 1L;

    @Value("${skill.economy.skill-base-cost:100}")
    private long skillBaseCost = 100L;

    @Value("${skill.economy.skill-step-cost:20}")
    private long skillStepCost = 20L;

    @Value("${skill.economy.talent-base-cost:120}")
    private long talentBaseCost = 120L;

    @Value("${skill.economy.talent-step-cost:25}")
    private long talentStepCost = 25L;

    @Value("${skill.economy.one-key-discount-percent:0}")
    private int oneKeyDiscountPercent = 0;

    // ═══════════════════════════════ SKILL ═══════════════════════════════

    /** Lấy toàn bộ kỹ năng của nhân vật. */
    @Transactional(readOnly = true)
    public SkillDTOs.SkillAllInfoResp getSkillAll(Long roleId) {
        if (roleId == null) {
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.OK)
                    .build();
        }
        List<RoleSkill> skills = skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(roleId);
        List<SkillDTOs.RoleSkillInfo> list = skills.stream()
                .map(s -> SkillDTOs.RoleSkillInfo.builder()
                        .skillId(s.getSkillId())
                        .skillLevel(s.getSkillLevel())
                        .skillIndex(s.getSkillIndex())
                        .build())
                .toList();
        return SkillDTOs.SkillAllInfoResp.builder()
                .skillCount(list.size())
                .skillList(list)
                .errorCode(SkillErrorCodes.OK)
                .build();
    }

    /**
     * Học kỹ năng mới hoặc nâng cấp kỹ năng đã có.
     * <ul>
     *   <li>Chưa có  → thêm mới (level=1, index=số kỹ năng hiện tại)</li>
     *   <li>Đã có    → tăng skill_level thêm 1 (tới max level từ config)</li>
     * </ul>
     * <p>Validate: roleId not null, skillId > 0, skillId tồn tại trong single_skill.json,
     * role exists, not already max level.
     */
    @Transactional
    public SkillDTOs.SkillAllInfoResp learnSkill(Long roleId, Integer skillId) {
        if (roleId == null || skillId == null || skillId <= 0) {
            log.warn("[skill] learnSkill invalid skillId={} roleId={}", skillId, roleId);
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("invalid skillId=" + skillId + " roleId=" + roleId)
                    .build();
        }
        if (!skillConfigCache.isValidSkillId(skillId)) {
            log.warn("[skill] learnSkill unknown skillId={} (not in config)", skillId);
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("skill not found in config: skillId=" + skillId)
                    .build();
        }
        if (!roleRepo.existsById(roleId)) {
            log.warn("[skill] learnSkill role not found roleId={}", roleId);
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.ROLE_NOT_FOUND)
                    .errorMsg("role not found: " + roleId)
                    .build();
        }
        final int maxSkillLevel = skillConfigCache.getSkillMaxLevel(skillId);
        final AtomicInteger resultCode = new AtomicInteger(SkillErrorCodes.OK);
        final AtomicReference<String> resultMsg = new AtomicReference<>(null);
        withRoleLock(roleId, () -> {
            skillRepo.findByRoleIdAndSkillId(roleId, skillId).ifPresentOrElse(
                    existing -> {
                        if (existing.getSkillLevel() < maxSkillLevel) {
                            int nextLevel = existing.getSkillLevel() + 1;
                            long cost = costForSkillLevel(nextLevel);
                            String idemKey = "skill.learn:" + roleId + ":" + skillId + ":" + nextLevel;
                            if (!tryWalletCost(roleId, cost, idemKey, 1470, 1, "skill.learn")) {
                                resultCode.set(SkillErrorCodes.INSUFFICIENT_RESOURCE);
                                resultMsg.set("wallet cost failed for skill level up");
                                return;
                            }
                            existing.setSkillLevel(existing.getSkillLevel() + 1);
                            skillRepo.save(existing);
                            log.info("[skill] levelUp roleId={} skillId={} newLevel={} maxLevel={}",
                                    roleId, skillId, existing.getSkillLevel(), maxSkillLevel);
                        } else {
                            log.info("[skill] alreadyMax roleId={} skillId={} maxLevel={}", roleId, skillId, maxSkillLevel);
                            resultCode.set(SkillErrorCodes.ALREADY_MAX_LEVEL);
                            resultMsg.set("skill already at max level");
                        }
                    },
                    () -> {
                        long cost = costForSkillLevel(1);
                        String idemKey = "skill.learn:" + roleId + ":" + skillId + ":1";
                        if (!tryWalletCost(roleId, cost, idemKey, 1470, 1, "skill.learn")) {
                            resultCode.set(SkillErrorCodes.INSUFFICIENT_RESOURCE);
                            resultMsg.set("wallet cost failed for learning new skill");
                            return;
                        }
                        int nextIndex = skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(roleId).size();
                        RoleSkill s = new RoleSkill();
                        s.setRoleId(roleId);
                        s.setSkillId(skillId);
                        s.setSkillLevel(1);
                        s.setSkillIndex(nextIndex);
                        skillRepo.save(s);
                        log.info("[skill] learn NEW roleId={} skillId={} index={}", roleId, skillId, nextIndex);
                    }
            );
        });
        SkillDTOs.SkillAllInfoResp resp = getSkillAll(roleId);
        if (resultCode.get() != SkillErrorCodes.OK) {
            resp.setErrorCode(resultCode.get());
            if (resultCode.get() == SkillErrorCodes.ALREADY_MAX_LEVEL) {
                resp.setErrorMsg("skill already at max level: skillId=" + skillId + " maxLevel=" + maxSkillLevel);
            } else {
                resp.setErrorMsg(resultMsg.get());
            }
        }
        return resp;
    }

    /**
     * Một phím nâng cấp tất cả kỹ năng (one-key level up).
     * Tăng tất cả kỹ năng có cấp &lt; max level lên 1 cấp.
     *
     * <p>Quy tắc one-key: chỉ tăng những kỹ năng chưa đạt cấp tối đa.
     * Nếu một kỹ năng đã max thì bỏ qua, không báo lỗi.
     * Max level mặc định toàn cục: {@code skill.config.default-max-level} (default 20).
     */
    @Transactional
    public SkillDTOs.SkillAllInfoResp oneKeyLevelUp(Long roleId) {
        if (roleId == null) {
            log.warn("[skill] oneKeyLevelUp invalid roleId=null");
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("roleId is null")
                    .build();
        }
        if (!roleRepo.existsById(roleId)) {
            log.warn("[skill] oneKeyLevelUp role not found roleId={}", roleId);
            return SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .skillList(List.of())
                    .errorCode(SkillErrorCodes.ROLE_NOT_FOUND)
                    .errorMsg("role not found: " + roleId)
                    .build();
        }
        AtomicInteger resultCode = new AtomicInteger(SkillErrorCodes.OK);
        AtomicReference<String> resultMsg = new AtomicReference<>(null);
        withRoleLock(roleId, () -> {
            List<RoleSkill> skills = skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(roleId);
            List<RoleSkill> upgradable = skills.stream()
                    .filter(s -> s.getSkillLevel() < skillConfigCache.getSkillMaxLevel(s.getSkillId()))
                    .toList();
            if (upgradable.isEmpty()) {
                return;
            }

            long totalCost = upgradable.stream()
                    .mapToLong(s -> costForSkillLevel(s.getSkillLevel() + 1))
                    .sum();
            totalCost = applyOneKeyDiscount(totalCost);

            String signature = upgradable.stream()
                    .sorted(Comparator.comparing(RoleSkill::getSkillId))
                    .map(s -> s.getSkillId() + "@" + s.getSkillLevel())
                    .reduce((l, r) -> l + "," + r)
                    .orElse("none");
            String idemKey = "skill.onekey:" + roleId + ":" + Integer.toHexString(signature.hashCode());

            if (!tryWalletCost(roleId, totalCost, idemKey, 1470, 2, "skill.one-key")) {
                resultCode.set(SkillErrorCodes.INSUFFICIENT_RESOURCE);
                resultMsg.set("wallet cost failed for one-key level up");
                return;
            }

            for (RoleSkill s : upgradable) {
                s.setSkillLevel(s.getSkillLevel() + 1);
                skillRepo.save(s);
            }
            log.info("[skill] oneKeyLevelUp roleId={} updated={} totalCost={}", roleId, upgradable.size(), totalCost);
        });
        SkillDTOs.SkillAllInfoResp resp = getSkillAll(roleId);
        if (resultCode.get() != SkillErrorCodes.OK) {
            resp.setErrorCode(resultCode.get());
            resp.setErrorMsg(resultMsg.get());
        }
        return resp;
    }

    // ═══════════════════════════════ TALENT ══════════════════════════════

    /** Lấy toàn bộ thiên phú của nhân vật. */
    @Transactional(readOnly = true)
    public SkillDTOs.TalentAllInfoResp getTalentAll(Long roleId) {
        if (roleId == null) {
            return SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(0)
                    .talentSkillList(List.of())
                    .errorCode(SkillErrorCodes.OK)
                    .build();
        }
        List<RoleTalent> talents = talentRepo.findByRoleIdOrderBySkillIdAsc(roleId);
        List<SkillDTOs.RoleTalentInfo> list = talents.stream()
                .map(t -> SkillDTOs.RoleTalentInfo.builder()
                        .skillId(t.getSkillId())
                        .skillLevel(t.getSkillLevel())
                        .build())
                .toList();
        return SkillDTOs.TalentAllInfoResp.builder()
                .talentSkillCount(list.size())
                .talentSkillList(list)
                .errorCode(SkillErrorCodes.OK)
                .build();
    }

    /**
     * Học thiên phú mới hoặc nâng cấp thiên phú đã có.
     * <ul>
     *   <li>Chưa có  → thêm mới (level=1)</li>
     *   <li>Đã có    → tăng level thêm 1 (tới max level từ config)</li>
     * </ul>
     * <p>Validate: roleId not null, skillId > 0, skillId tồn tại trong passive_skill.json,
     * role exists, not already max level.
     */
    @Transactional
    public SkillDTOs.TalentAllInfoResp learnTalent(Long roleId, Integer skillId) {
        if (roleId == null || skillId == null || skillId <= 0) {
            log.warn("[talent] learnTalent invalid skillId={} roleId={}", skillId, roleId);
            return SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(0)
                    .talentSkillList(List.of())
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("invalid skillId=" + skillId + " roleId=" + roleId)
                    .build();
        }
        if (!skillConfigCache.isValidTalentId(skillId)) {
            log.warn("[talent] learnTalent unknown skillId={} (not in config)", skillId);
            return SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(0)
                    .talentSkillList(List.of())
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("talent not found in config: skillId=" + skillId)
                    .build();
        }
        if (!roleRepo.existsById(roleId)) {
            log.warn("[talent] learnTalent role not found roleId={}", roleId);
            return SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(0)
                    .talentSkillList(List.of())
                    .errorCode(SkillErrorCodes.ROLE_NOT_FOUND)
                    .errorMsg("role not found: " + roleId)
                    .build();
        }
        final int maxTalentLevel = skillConfigCache.getTalentMaxLevel(skillId);
        final AtomicInteger resultCode = new AtomicInteger(SkillErrorCodes.OK);
        final AtomicReference<String> resultMsg = new AtomicReference<>(null);
        withRoleLock(roleId, () -> {
            talentRepo.findByRoleIdAndSkillId(roleId, skillId).ifPresentOrElse(
                    existing -> {
                        if (existing.getSkillLevel() < maxTalentLevel) {
                            int nextLevel = existing.getSkillLevel() + 1;
                            long cost = costForTalentLevel(nextLevel);
                            String idemKey = "talent.learn:" + roleId + ":" + skillId + ":" + nextLevel;
                            if (!tryWalletCost(roleId, cost, idemKey, 1480, 1, "talent.learn")) {
                                resultCode.set(SkillErrorCodes.INSUFFICIENT_RESOURCE);
                                resultMsg.set("wallet cost failed for talent level up");
                                return;
                            }
                            existing.setSkillLevel(existing.getSkillLevel() + 1);
                            talentRepo.save(existing);
                            log.info("[talent] levelUp roleId={} skillId={} newLevel={} maxLevel={}",
                                    roleId, skillId, existing.getSkillLevel(), maxTalentLevel);
                        } else {
                            log.info("[talent] alreadyMax roleId={} skillId={} maxLevel={}", roleId, skillId, maxTalentLevel);
                            resultCode.set(SkillErrorCodes.ALREADY_MAX_LEVEL);
                            resultMsg.set("talent already at max level");
                        }
                    },
                    () -> {
                        long cost = costForTalentLevel(1);
                        String idemKey = "talent.learn:" + roleId + ":" + skillId + ":1";
                        if (!tryWalletCost(roleId, cost, idemKey, 1480, 1, "talent.learn")) {
                            resultCode.set(SkillErrorCodes.INSUFFICIENT_RESOURCE);
                            resultMsg.set("wallet cost failed for learning new talent");
                            return;
                        }
                        RoleTalent t = new RoleTalent();
                        t.setRoleId(roleId);
                        t.setSkillId(skillId);
                        t.setSkillLevel(1);
                        talentRepo.save(t);
                        log.info("[talent] learn NEW roleId={} skillId={}", roleId, skillId);
                    }
            );
        });
        SkillDTOs.TalentAllInfoResp resp = getTalentAll(roleId);
        if (resultCode.get() != SkillErrorCodes.OK) {
            resp.setErrorCode(resultCode.get());
            if (resultCode.get() == SkillErrorCodes.ALREADY_MAX_LEVEL) {
                resp.setErrorMsg("talent already at max level: skillId=" + skillId + " maxLevel=" + maxTalentLevel);
            } else {
                resp.setErrorMsg(resultMsg.get());
            }
        }
        return resp;
    }

    private void withRoleLock(Long roleId, Runnable action) {
        Object roleLock = roleLocks.computeIfAbsent(roleId, key -> new Object());
        synchronized (roleLock) {
            action.run();
        }
    }

    private long costForSkillLevel(int nextLevel) {
        int lv = Math.max(1, nextLevel);
        return Math.max(0L, skillBaseCost + (long) (lv - 1) * skillStepCost);
    }

    private long costForTalentLevel(int nextLevel) {
        int lv = Math.max(1, nextLevel);
        return Math.max(0L, talentBaseCost + (long) (lv - 1) * talentStepCost);
    }

    private long applyOneKeyDiscount(long amount) {
        if (amount <= 0) return 0;
        int percent = Math.max(0, Math.min(80, oneKeyDiscountPercent));
        if (percent == 0) return amount;
        return Math.max(0, amount - (amount * percent / 100));
    }

    private boolean tryWalletCost(Long roleId, long amount, String idemKey, int reason, int reasonType, String op) {
        if (!economyEnabled || amount <= 0) {
            return true;
        }
        try {
            WalletDTOs.BatchReq req = WalletDTOs.BatchReq.builder()
                    .roleId(String.valueOf(roleId))
                    .changes(List.of(WalletDTOs.Change.builder()
                            .itemId(economyCurrencyItemId)
                            .amount(amount)
                            .build()))
                    .idemKey(idemKey)
                    .reason(reason)
                    .reasonType(reasonType)
                    .build();

            ResultDTO<WalletDTOs.MutateResp> result = walletFeign.batchCost(req);
            if (result == null) {
                log.warn("[skill-wallet] {} roleId={} amount={} -> null response", op, roleId, amount);
                return false;
            }
            if (result.getCode() != 0 || result.getData() == null) {
                log.warn("[skill-wallet] {} roleId={} amount={} -> code={} msg={}",
                        op, roleId, amount, result.getCode(), result.getMessage());
                return false;
            }
            boolean ok = result.getData().isOk();
            if (!ok) {
                log.warn("[skill-wallet] {} roleId={} amount={} -> wallet error={}",
                        op, roleId, amount, result.getData().getError());
            }
            return ok;
        } catch (Exception ex) {
            log.warn("[skill-wallet] {} roleId={} amount={} failed: {}", op, roleId, amount, ex.toString());
            return false;
        }
    }
}

