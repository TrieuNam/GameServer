package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.dto.PlayerStats;
import com.SouthMillion.battleserver_service.service.client.MonsterConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonsterStatsService {

    private static final String MONSTER_CONFIG_PATH = "gameworld/monster/monster.json";

    private final MonsterConfigFeign monsterConfigFeign;

    private volatile Map<Integer, JsonNode> monsterCache = Map.of();

    public PlayerStats getMonsterStats(Long fallbackRoleId, Integer monsterId, Integer stageId, boolean boss) {
        if (monsterId == null || monsterId <= 0) {
            return buildFallbackStats(fallbackRoleId, 0, stageId, boss);
        }

        try {
            JsonNode monsterNode = getMonsterCache().get(monsterId);
            if (monsterNode == null) {
                log.warn("[combat] monsterId={} not found in config, using fallback stats", monsterId);
                return buildFallbackStats(fallbackRoleId, monsterId, stageId, boss);
            }
            return toPlayerStats(fallbackRoleId, monsterId, stageId, boss, monsterNode);
        } catch (Exception e) {
            log.warn("[combat] fallback monster stats for monsterId={} due to {}", monsterId, e.toString());
            return buildFallbackStats(fallbackRoleId, monsterId, stageId, boss);
        }
    }

    private Map<Integer, JsonNode> getMonsterCache() {
        if (!monsterCache.isEmpty()) {
            return monsterCache;
        }

        synchronized (this) {
            if (!monsterCache.isEmpty()) {
                return monsterCache;
            }

            Map<Integer, JsonNode> loaded = new HashMap<>();
            JsonNode root = monsterConfigFeign.getFile(MONSTER_CONFIG_PATH);
            collectMonsterNodes(root, loaded);
            monsterCache = loaded;
            return monsterCache;
        }
    }

    private void collectMonsterNodes(JsonNode node, Map<Integer, JsonNode> loaded) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectMonsterNodes(child, loaded);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        int monsterId = node.path("monster_id").asInt(0);
        if (monsterId > 0) {
            loaded.put(monsterId, node);
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            JsonNode child = fields.next().getValue();
            if (child != null && child.isContainerNode()) {
                collectMonsterNodes(child, loaded);
            }
        }
    }

    private PlayerStats toPlayerStats(Long fallbackRoleId, Integer monsterId, Integer stageId, boolean boss, JsonNode node) {
        double stageMultiplier = 1.0 + Math.max(0, valueOrZero(stageId) - 1) * 0.02;
        double hpMultiplier = boss ? stageMultiplier * 1.5 : stageMultiplier;
        double attackMultiplier = boss ? stageMultiplier * 1.25 : stageMultiplier;
        double defenseMultiplier = boss ? stageMultiplier * 1.20 : stageMultiplier;
        double speedMultiplier = boss ? 1.10 : 1.0;

        int hp = scale(stat(node, "hp", boss ? 1800 : 1000), hpMultiplier);
        int attack = scale(stat(node, "attack", boss ? 220 : 140), attackMultiplier);
        int defense = scale(stat(node, "defense", boss ? 80 : 50), defenseMultiplier);
        int speed = scale(stat(node, "speed", boss ? 120 : 80), speedMultiplier);

        return PlayerStats.builder()
                .playerId(resolveEntityId(fallbackRoleId, monsterId))
                .hp(hp)
                .maxHp(hp)
                .attack(attack)
                .defense(defense)
                .speed(speed)
                .critRate(stat(node, "baoji", 0))
                .critDamage(200)
                .vampiric(stat(node, "xixue", 0))
                .vampiricImmunity(stat(node, "de_xixue", 0))
                .counter(stat(node, "fanji", 0))
                .counterImmunity(stat(node, "de_fanji", 0))
                .combo(stat(node, "lianji", 0))
                .comboImmunity(stat(node, "de_lianji", 0))
                .evasion(stat(node, "shanbi", 0))
                .evasionImmunity(stat(node, "de_shanbi", 0))
                .criticalImmunity(stat(node, "de_baoji", 0))
                .stun(stat(node, "jiyun", 0))
                .stunImmunity(stat(node, "de_jiyun", 0))
                .tyranny(0)
                .benevolence(0)
                .muddy(0)
                .interdiction(0)
                .rejuvenation(0)
                .level(Math.max(1, stat(node, "monster_level", Math.max(1, valueOrZero(stageId)))))
                .fightPower(hp + (attack * 5) + (defense * 3) + (speed * 2))
                .build();
    }

    private PlayerStats buildFallbackStats(Long fallbackRoleId, Integer monsterId, Integer stageId, boolean boss) {
        int hp = boss ? 1800 : 1000;
        int attack = boss ? 220 : 140;
        int defense = boss ? 80 : 50;
        int speed = boss ? 120 : 80;

        if (stageId != null && stageId > 1) {
            double multiplier = 1.0 + ((stageId - 1) * 0.02);
            hp = scale(hp, multiplier);
            attack = scale(attack, multiplier);
            defense = scale(defense, multiplier);
        }

        return PlayerStats.builder()
                .playerId(resolveEntityId(fallbackRoleId, monsterId))
                .hp(hp)
                .maxHp(hp)
                .attack(attack)
                .defense(defense)
                .speed(speed)
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
                .level(Math.max(1, valueOrZero(stageId)))
                .fightPower(hp + (attack * 5) + (defense * 3) + (speed * 2))
                .build();
    }

    private Long resolveEntityId(Long fallbackRoleId, Integer monsterId) {
        if (fallbackRoleId != null && fallbackRoleId > 0) {
            return fallbackRoleId;
        }
        return monsterId != null ? monsterId.longValue() : 0L;
    }

    private int stat(JsonNode node, String field, int fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isNumber()) {
            return Math.max(0, value.asInt(fallback));
        }
        if (value.isTextual()) {
            try {
                return Math.max(0, Integer.parseInt(value.asText().trim()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private int scale(int value, double multiplier) {
        return Math.max(1, (int) Math.round(value * multiplier));
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}
