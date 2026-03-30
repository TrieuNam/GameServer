package com.SouthMillion.role_service.service;


import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.mapper.RoleMapper;
import com.SouthMillion.role_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * RoleService: khởi tạo chỉ số từ base_att, cộng exp theo LevelTable, giới hạn maxLevel.
 * Giao diện hàm giữ nguyên như bạn đang dùng để tránh phá flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repo;
    private final RoleConfigCache cfg;
    private final NewbieGiftService NewbieGiftService;

    /** Optional: injected lazily so the service still starts if Kafka is misconfigured */
    @Autowired(required = false)
    @Qualifier("objectKafkaTemplate")
    private KafkaTemplate<String, Object> objectKafkaTemplate;

    @Cacheable(cacheNames = "roleById", key = "#roleId")
    public Optional<RoleDTOs.RoleResp> getById(Long roleId) {
        return repo.findById(roleId).map(RoleMapper::toResp);
    }

    public List<RoleDTOs.RoleResp> listByUserId(String userId) {
        return repo.findByUserIdOrderByRoleIdAsc(userId)
                .stream().map(RoleMapper::toResp).toList();
    }

    @Transactional
    public RoleDTOs.RoleResp create(RoleDTOs.CreateRoleReq req) {
        // Tên: ưu tiên request; nếu trống dùng pool từ rolename.json
        String nameBase = StringUtils.hasText(req.getName()) ? req.getName().trim() : cfg.randomName();
        String finalName = ensureUniqueName(req.getUserId(), nameBase);

        // Đọc config
        RoleConfigCache.LevelTable lv = cfg.levelTable();
        RoleConfigCache.BaseAttrCfg base = cfg.baseAttr();

        Role r = new Role();
        r.setUserId(req.getUserId());
        r.setName(finalName);

        // Level khởi tạo theo minLevel của bảng (robust hơn hardcode 1)
        r.setLevel(Math.max(1, lv.getMinLevel()));
        r.setExp(0);

        // Base attributes từ base_att trong roleexp.json
        r.setHp(safeInt(base.getHp()));
        r.setAttackValue(safeInt(base.getAttack()));
        r.setDefenseValue(safeInt(base.getDefend()));
        r.setSpeed(safeInt(base.getSpeed()));

        try {
            repo.saveAndFlush(r);
        } catch (DataIntegrityViolationException ex) {
            // Va chạm unique name cùng user → thử lại với hậu tố
            r.setName(ensureUniqueName(req.getUserId(), nameBase));
            repo.saveAndFlush(r);
        }
        NewbieGiftService.onFirstLogin(r.getUserId(), String.valueOf(r.getRoleId()));
        return RoleMapper.toResp(r);
    }

    @Transactional
    @CachePut(cacheNames = "roleById", key = "#roleId")
    public RoleDTOs.RoleResp addExp(Long roleId, long add) {
        Role r = repo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        if (add <= 0) return RoleMapper.toResp(r);

        RoleConfigCache.LevelTable lv = cfg.levelTable();
        long expToAdd = add;
        int levelsGained = 0;

        while (r.getLevel() < lv.getMaxLevel() && expToAdd > 0) {
            long need = lv.expRequiredFor(r.getLevel()); // exp cần để lên level tiếp theo
            if (need <= 0) break; // guard

            long cur = r.getExp();
            if (cur + expToAdd < need) {
                r.setExp(cur + expToAdd);
                expToAdd = 0;
            } else {
                long used = need - cur;
                expToAdd -= used;
                r.setExp(0);
                r.setLevel(r.getLevel() + 1);
                levelsGained++;

                // Tăng chỉ số mỗi lần lên cấp theo increment của LevelTable
                r.setHp(safeAdd((int) r.getHp(), lv.getHpPerLv()));
                r.setAttackValue(safeAdd((int) r.getAttackValue(), lv.getAtkPerLv()));
                r.setDefenseValue(safeAdd((int) r.getDefenseValue(), lv.getDefPerLv()));
                r.setSpeed(safeAdd(r.getSpeed(), lv.getSpdPerLv()));
            }
        }

        // Đã max level thì bỏ qua phần exp dư (parity với C++: không vượt trần)
        repo.saveAndFlush(r);

        if (levelsGained > 0 && objectKafkaTemplate != null) {
            try {
                objectKafkaTemplate.send("role.level.up",
                        Map.of("roleId", roleId, "levelsGained", levelsGained, "newLevel", r.getLevel()));
            } catch (Exception e) {
                log.warn("[RoleService] Failed to publish role.level.up for roleId={}: {}", roleId, e.getMessage());
            }
        }

        return RoleMapper.toResp(r);
    }

    @Transactional
    @CachePut(cacheNames = "roleById", key = "#roleId")
    public RoleDTOs.RoleResp rename(Long roleId, String newNameRaw) {
        if (!StringUtils.hasText(newNameRaw)) {
            throw new IllegalArgumentException("Tên nhân vật không được trống");
        }
        Role r = repo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        String base = newNameRaw.trim();
        String finalName = ensureUniqueName(r.getUserId(), base);
        r.setName(finalName);
        try {
            repo.saveAndFlush(r);
        } catch (DataIntegrityViolationException ex) {
            r.setName(ensureUniqueName(r.getUserId(), base));
            repo.saveAndFlush(r);
        }
        return RoleMapper.toResp(r);
    }

    @Transactional
    @CachePut(cacheNames = "roleById", key = "#roleId")
    public RoleDTOs.RoleResp setWxInfo(Long roleId, String name, String headChar) {
        Role r = repo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        if (StringUtils.hasText(name)) {
            String finalName = ensureUniqueName(r.getUserId(), name.trim());
            r.setName(finalName);
        }
        if (StringUtils.hasText(headChar)) {
            r.setHeadChar(headChar.trim());
        }
        repo.saveAndFlush(r);
        return RoleMapper.toResp(r);
    }

    @Transactional
    @CachePut(cacheNames = "roleById", key = "#roleId")
    public RoleDTOs.RoleResp applyStatDelta(Long roleId, long hpDelta, long attackDelta, long defenseDelta, int speedDelta) {
        Role r = repo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        r.setHp(Math.max(0, r.getHp() + hpDelta));
        r.setAttackValue(Math.max(0, r.getAttackValue() + attackDelta));
        r.setDefenseValue(Math.max(0, r.getDefenseValue() + defenseDelta));
        r.setSpeed(Math.max(0, safeAdd(r.getSpeed(), speedDelta)));

        repo.saveAndFlush(r);
        return RoleMapper.toResp(r);
    }

    @CacheEvict(cacheNames = "roleById", key = "#roleId")
    public void evictCache(Long roleId) {
        // no-op
    }

    // ================= helpers =================

    private String ensureUniqueName(String userId, String base) {
        String candidate = (base == null ? "" : base.trim());
        if (candidate.isEmpty()) candidate = cfg.randomName();

        int tryCount = 0;
        while (repo.findByUserIdAndName(userId, candidate).isPresent()) {
            tryCount++;
            candidate = base + "_" + java.util.concurrent.ThreadLocalRandom.current()
                    .nextInt(100, 10000);
            if (tryCount > 10) break;
        }
        return candidate;
    }

    private int safeInt(long v) {
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) v;
    }
    private int safeAdd(int cur, long inc) {
        long sum = (long) cur + inc;
        return safeInt(sum);
    }

    /**
     * Combat power for battle simulation.
     * Formula (simplified until equip/gear stats are wired):
     *   fightPower = level * 150 + level * level * 2
     *   hp  = level * 500
     *   atk = level * 80
     *   def = level * 40
     *   spd = level * 20 + 100
     */
    public java.util.Map<String, Object> getCombatPower(Long roleId) {
        var role = repo.findById(roleId).orElse(null);
        int level = (role != null && role.getLevel() > 0) ? role.getLevel() : 1;
        long fp  = (long) level * 150 + (long) level * level * 2;
        long hp  = (long) level * 500;
        long atk = (long) level * 80;
        long def = (long) level * 40;
        long spd = (long) level * 20 + 100;
        return java.util.Map.of(
                "fightPower", fp,
                "hp", hp, "atk", atk, "def", def, "spd", spd,
                "level", level
        );
    }

    /**
     * Legacy notice-time parity for Msg 1464/1465.
     * type=0: query current value; type=1: set notice_time using param.
     */
    @Transactional
    public long noticeTime(Long roleId, int type, long param) {
        if (roleId == null || roleId <= 0) return 0L;
        Role role = repo.findById(roleId).orElse(null);
        if (role == null) return 0L;
        if (type == 1) {
            long v = Math.max(0L, param);
            role.setNoticeTime(v);
            repo.saveAndFlush(role);
            return v;
        }
        return role.getNoticeTime() != null ? role.getNoticeTime() : 0L;
    }
}