package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repo;
    private final RoleConfigCache cfg;
    private final CacheManager cacheManager;

    // ===== Query =====
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "role:listByUser", key = "#userId")
    public RoleDTOs.ListResp listByUser(String userId) {
        var items = Optional.ofNullable(
                        repo.findByUserIdOrderByRoleIdAsc(userId)
                ).orElseGet(List::of)
                .stream().map(this::toResp).toList();
        return RoleDTOs.ListResp.builder().items(items).build();
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "role:detail", key = "#roleId")
    public RoleDTOs.RoleResp detail(String roleId) {
        return repo.findById(roleId).map(this::toResp).orElse(null);
    }

    // ===== Create =====
    @Transactional
    public RoleDTOs.RoleResp create(RoleDTOs.CreateRoleReq req) {
        cfg.refreshAllIfNeeded();

        String baseName = firstNonBlank(req.getName(), req.getNickname(), req.getRoleName());
        if (!StringUtils.hasText(baseName)) baseName = cfg.generateRandomNameFromPool();

        String uniqueName = ensureUniqueName(req.getUserId(), baseName);
        var d = cfg.defaults();

        Role r = new Role();
        r.setUserId(req.getUserId());
        r.setName(uniqueName);
        r.setLevel(1);
        r.setExp(0L);

        r.setHp(d.getBaseHp());
        r.setAttack(d.getBaseAtk());
        r.setDefense(d.getBaseDef());
        r.setSpeed(d.getBaseSpd());

        r.setCap(null);
        r.setHeadPicId(null);
        r.setTitleId(null);
        r.setKnightLevel(null);
        r.setGuildName(null);
        r.setHeadChar(null);

        try {
            repo.save(r);
        } catch (DataIntegrityViolationException ex) {
            for (int i = 0; i < 3; i++) {
                uniqueName = baseName + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
                if (repo.findByUserIdAndName(req.getUserId(), uniqueName).isEmpty()) {
                    r.setName(uniqueName);
                    repo.save(r);
                    evictCachesFor(r);
                    return toResp(r);
                }
            }
            throw ex;
        }

        evictCachesFor(r);
        return toResp(r);
    }

    // ===== Mutations =====
    @Transactional
    public RoleDTOs.RoleResp addExp(String roleId, long addExp) {
        if (addExp <= 0) return detail(roleId);
        cfg.refreshRoleExpIfNeeded();

        Role r = repo.findById(roleId).orElse(null);
        if (r == null) return null;

        long exp = Math.addExact(r.getExp(), addExp);
        int maxLv = cfg.maxLevel();

        boolean leveled = false;
        while (r.getLevel() < maxLv) {
            long need = cfg.needExp(r.getLevel());
            if (exp >= need) {
                exp -= need;
                r.setLevel(r.getLevel() + 1);

                var d = cfg.defaults();
                r.setHp(r.getHp() + d.getHpPerLv());
                r.setAttack(r.getAttack() + d.getAtkPerLv());
                r.setDefense(r.getDefense() + d.getDefPerLv());
                r.setSpeed(r.getSpeed() + d.getSpdPerLv());
                leveled = true;
            } else break;
        }
        r.setExp(exp);
        repo.save(r);

        if (leveled) log.debug("role {} -> level {} (exp={})", r.getRoleId(), r.getLevel(), r.getExp());
        evictCachesFor(r);
        return toResp(r);
    }

    @Transactional
    public RoleDTOs.RoleResp rename(String roleId, String newName) {
        if (!StringUtils.hasText(newName)) return detail(roleId);

        Role r = repo.findById(roleId).orElse(null);
        if (r == null) return null;

        String unique = ensureUniqueName(r.getUserId(), newName.trim());
        r.setName(unique);
        repo.save(r);
        evictCachesFor(r);
        return toResp(r);
    }

    // ===== Helpers =====
    private void evictCachesFor(Role r) {
        if (cacheManager.getCache("role:listByUser") != null)
            cacheManager.getCache("role:listByUser").evict(r.getUserId());
        if (cacheManager.getCache("role:detail") != null)
            cacheManager.getCache("role:detail").evict(r.getRoleId());
    }

    private RoleDTOs.RoleResp toResp(Role r) {
        return RoleDTOs.RoleResp.builder()
                .roleId(r.getRoleId())
                .userId(r.getUserId())
                .name(r.getName()).nickname(null).roleName(null)
                .level(r.getLevel()).curExp(r.getExp())
                .cap(r.getCap()).headPicId(r.getHeadPicId()).titleId(r.getTitleId())
                .createTimeEpochSec(r.getCreatedAt() == null ? null : r.getCreatedAt().getEpochSecond())
                .knightLevel(r.getKnightLevel()).headChar(r.getHeadChar()).guildName(r.getGuildName())
                .hp(r.getHp()).attack(r.getAttack()).defense(r.getDefense()).speed(r.getSpeed())
                .build();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String s : vals) if (StringUtils.hasText(s)) return s.trim();
        return null;
    }

    private String ensureUniqueName(String userId, String base) {
        String candidate = (base == null ? "" : base.trim());
        if (candidate.isEmpty()) candidate = "Player_" + ThreadLocalRandom.current().nextInt(1000, 10000);

        for (int i = 0; i < 5 && repo.findByUserIdAndName(userId, candidate).isPresent(); i++) {
            candidate = base + "_" + ThreadLocalRandom.current().nextInt(100, 1000);
        }
        while (repo.findByUserIdAndName(userId, candidate).isPresent()) {
            candidate = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidate;
    }
}