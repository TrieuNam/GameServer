package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repo;
    private final RoleConfigCache cfg;

    @Transactional()
    public RoleDTOs.ListResp listByUser(String userId) {
        var list = repo.findByUserIdOrderByRoleIdAsc(userId).stream().map(this::toResp).toList();
        return RoleDTOs.ListResp.builder().items(list).build();
    }

    @Transactional()
    public RoleDTOs.RoleResp detail(String roleId) {
        return repo.findById(roleId).map(this::toResp).orElse(null);
    }

    @Transactional
    public RoleDTOs.RoleResp create(RoleDTOs.CreateRoleReq req) {
        cfg.refreshAllIfNeeded();

        // 1) Lấy baseName
        String baseName = StringUtils.hasText(req.getName())
                ? req.getName().trim()
                : generateRandomName(req.getUserId());

        // 2) Bảo đảm duy nhất trong phạm vi userId
        String uniqueName = ensureUniqueName(req.getUserId(), baseName);

        var d = cfg.defaults();

        Role r = new Role();
        r.setUserId(req.getUserId());
        r.setName(uniqueName);
        r.setLevel(1);
        r.setExp(0);
        r.setHp(d.getBaseHp());
        r.setAttack(d.getBaseAtk());
        r.setDefense(d.getBaseDef());
        r.setSpeed(d.getBaseSpd());

        // (Tuỳ chọn) nếu DB có unique index (user_id, name), retry nhẹ khi đụng race
        try {
            repo.save(r);
        } catch (DataIntegrityViolationException ex) {
            // thử đổi hậu tố vài lần rồi lưu lại
            for (int i = 0; i < 3; i++) {
                uniqueName = baseName + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
                if (repo.findByUserIdAndName(req.getUserId(), uniqueName).isEmpty()) {
                    r.setName(uniqueName);
                    repo.save(r);
                    return toResp(r);
                }
            }
            throw ex; // hết retry thì ném lỗi
        }

        return toResp(r);
    }

    /**
     * Tạo tên ngẫu nhiên nếu user không gửi name.
     */
    private String generateRandomName(String userIdMaybe) {
        var pool = cfg.namePool();
        String prefix = pool.isEmpty() ? "Player" : pool.get((int) (System.nanoTime() % pool.size()));
        String suffix = Integer.toString(ThreadLocalRandom.current().nextInt(1000, 10000));
        return prefix + "_" + suffix;
    }

    /**
     * Đảm bảo name là duy nhất trong phạm vi userId.
     */
    private String ensureUniqueName(String userId, String base) {
        String candidate = (base == null ? "" : base.trim());
        if (candidate.isEmpty()) candidate = "Player_" + ThreadLocalRandom.current().nextInt(1000, 10000);

        // thử tối đa 5 lần hậu tố 3 chữ số
        for (int i = 0; i < 5 && repo.findByUserIdAndName(userId, candidate).isPresent(); i++) {
            candidate = base + "_" + ThreadLocalRandom.current().nextInt(100, 1000);
        }
        // nếu vẫn trùng, nâng lên 4 chữ số đến khi khác
        while (repo.findByUserIdAndName(userId, candidate).isPresent()) {
            candidate = base + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidate;
    }

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
                // tăng stat theo defaults
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
        return toResp(r);
    }

    @Transactional
    public RoleDTOs.RoleResp rename(String roleId, String newName) {
        if (!StringUtils.hasText(newName)) return detail(roleId);
        Role r = repo.findById(roleId).orElse(null);
        if (r == null) return null;
        r.setName(newName.trim());
        repo.save(r);
        return toResp(r);
    }

    private RoleDTOs.RoleResp toResp(Role r) {
        return RoleDTOs.RoleResp.builder()
                .roleId(r.getRoleId())
                .userId(r.getUserId())
                .name(r.getName())
                .level(r.getLevel())
                .exp(r.getExp())
                .hp(r.getHp())
                .attack(r.getAttack())
                .defense(r.getDefense())
                .speed(r.getSpeed())
                .build();
    }
}
