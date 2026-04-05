package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.dto.PlayerStats;
import com.SouthMillion.battleserver_service.service.client.RoleStatsFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleStatsService {

    private static final int ATTR_HP = 1;
    private static final int ATTR_ATTACK = 2;
    private static final int ATTR_DEFENSE = 3;
    private static final int ATTR_SPEED = 4;
    private static final int ATTR_VAMPIRIC = 6;
    private static final int ATTR_COUNTER = 7;
    private static final int ATTR_COMBO = 8;
    private static final int ATTR_EVASION = 9;
    private static final int ATTR_CRITICAL = 10;
    private static final int ATTR_STUN = 11;
    private static final int ATTR_VAMPIRIC_IMMUNITY = 12;
    private static final int ATTR_COUNTER_IMMUNITY = 13;
    private static final int ATTR_COMBO_IMMUNITY = 14;
    private static final int ATTR_EVASION_IMMUNITY = 15;
    private static final int ATTR_CRITICAL_IMMUNITY = 16;
    private static final int ATTR_STUN_IMMUNITY = 17;
    private static final int ATTR_TYRANNY = 18;
    private static final int ATTR_BENEVOLENCE = 19;
    private static final int ATTR_MUDDY = 20;
    private static final int ATTR_INTERDICTION = 21;
    private static final int ATTR_REJUVENATION = 22;
    private static final int ATTR_HP_PER = 27;
    private static final int ATTR_ATTACK_PER = 28;
    private static final int ATTR_DEFENSE_PER = 29;
    private static final int ATTR_SPEED_PER = 30;

    private final RoleStatsFeign roleStatsFeign;

    public PlayerStats getPlayerStats(Long roleId) {
        if (roleId == null) {
            return buildFallbackStats(0L);
        }

        try {
            OtherRoleDTOs.OtherRoleInfo info = roleStatsFeign.getOtherRole("role-" + roleId, String.valueOf(roleId));
            return toPlayerStats(roleId, info);
        } catch (Exception e) {
            log.warn("[combat] fallback combat stats for roleId={} due to {}", roleId, e.toString());
            return buildFallbackStats(roleId);
        }
    }

    private PlayerStats toPlayerStats(Long roleId, OtherRoleDTOs.OtherRoleInfo info) {
        if (info == null || info.attributes() == null) {
            return buildFallbackStats(roleId);
        }

        Map<Integer, Long> totals = new HashMap<>();
        if (info.roleAttrList() != null) {
            for (OtherRoleDTOs.OtherRoleAttrPair pair : info.roleAttrList()) {
                if (pair == null || pair.attrType() <= 0) {
                    continue;
                }
                totals.merge(pair.attrType(), Math.max(0L, pair.attrValue()), Long::sum);
            }
        }

        OtherRoleDTOs.OtherRoleAttr base = info.attributes();
        long hpBase = firstPositive(totals.get(ATTR_HP), Math.max(1L, base.hp()));
        long attackBase = firstPositive(totals.get(ATTR_ATTACK), Math.max(1L, base.attackValue()));
        long defenseBase = firstPositive(totals.get(ATTR_DEFENSE), Math.max(0L, base.defenseValue()));
        long speedBase = firstPositive(totals.get(ATTR_SPEED), Math.max(0L, base.speed()));

        long hp = applyBasisPoints(hpBase, totals.getOrDefault(ATTR_HP_PER, 0L));
        long attack = applyBasisPoints(attackBase, totals.getOrDefault(ATTR_ATTACK_PER, 0L));
        long defense = applyBasisPoints(defenseBase, totals.getOrDefault(ATTR_DEFENSE_PER, 0L));
        long speed = applyBasisPoints(speedBase, totals.getOrDefault(ATTR_SPEED_PER, 0L));

        long capability = info.capability() != null ? info.capability() : hp + attack + defense + (speed * 10L);

        return PlayerStats.builder()
                .playerId(roleId)
                .hp(safeInt(hp))
                .maxHp(safeInt(hp))
                .attack(safeInt(attack))
                .defense(safeInt(defense))
                .speed(safeInt(speed))
                .critRate(safeInt(totals.getOrDefault(ATTR_CRITICAL, 0L)))
                .critDamage(200)
                .vampiric(safeInt(totals.getOrDefault(ATTR_VAMPIRIC, 0L)))
                .vampiricImmunity(safeInt(totals.getOrDefault(ATTR_VAMPIRIC_IMMUNITY, 0L)))
                .counter(safeInt(totals.getOrDefault(ATTR_COUNTER, 0L)))
                .counterImmunity(safeInt(totals.getOrDefault(ATTR_COUNTER_IMMUNITY, 0L)))
                .combo(safeInt(totals.getOrDefault(ATTR_COMBO, 0L)))
                .comboImmunity(safeInt(totals.getOrDefault(ATTR_COMBO_IMMUNITY, 0L)))
                .evasion(safeInt(totals.getOrDefault(ATTR_EVASION, 0L)))
                .evasionImmunity(safeInt(totals.getOrDefault(ATTR_EVASION_IMMUNITY, 0L)))
                .criticalImmunity(safeInt(totals.getOrDefault(ATTR_CRITICAL_IMMUNITY, 0L)))
                .stun(safeInt(totals.getOrDefault(ATTR_STUN, 0L)))
                .stunImmunity(safeInt(totals.getOrDefault(ATTR_STUN_IMMUNITY, 0L)))
                .tyranny(safeInt(totals.getOrDefault(ATTR_TYRANNY, 0L)))
                .benevolence(safeInt(totals.getOrDefault(ATTR_BENEVOLENCE, 0L)))
                .muddy(safeInt(totals.getOrDefault(ATTR_MUDDY, 0L)))
                .interdiction(safeInt(totals.getOrDefault(ATTR_INTERDICTION, 0L)))
                .rejuvenation(safeInt(totals.getOrDefault(ATTR_REJUVENATION, 0L)))
                .level(Math.max(1, base.level()))
                .fightPower(safeInt(capability))
                .build();
    }

    private PlayerStats buildFallbackStats(Long roleId) {
        return PlayerStats.builder()
                .playerId(roleId)
                .hp(1000)
                .maxHp(1000)
                .attack(150)
                .defense(50)
                .speed(100)
                .critRate(0)
                .critDamage(200)
                .vampiric(0)
                .vampiricImmunity(0)
                .counter(0)
                .counterImmunity(0)
                .combo(0)
                .comboImmunity(0)
                .evasion(0)
                .evasionImmunity(0)
                .criticalImmunity(0)
                .stun(0)
                .stunImmunity(0)
                .tyranny(0)
                .benevolence(0)
                .muddy(0)
                .interdiction(0)
                .rejuvenation(0)
                .level(1)
                .fightPower(1300)
                .build();
    }

    private long firstPositive(Long preferred, long fallback) {
        if (preferred != null && preferred > 0) {
            return preferred;
        }
        return Math.max(0L, fallback);
    }

    private long applyBasisPoints(long base, long basisPoints) {
        if (base <= 0) {
            return 0L;
        }
        if (basisPoints <= 0) {
            return base;
        }
        return Math.max(1L, Math.round(base * (1.0 + (basisPoints / 10000.0))));
    }

    private int safeInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }
}
