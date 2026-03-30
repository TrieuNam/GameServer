package com.SouthMillion.role_service.config;

import com.SouthMillion.role_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SkillConfigCache
 *
 * <p>Nạp & cache:
 * <ul>
 *   <li>{@code single_skill.json} — config kỹ năng chủ động (active skill)</li>
 *   <li>{@code passive_skill.json} — config thiên phú kỹ năng (passive / talent skill)</li>
 * </ul>
 *
 * <p>Tích hợp với {@code config-service} qua Feign + ETag caching (304 nếu chưa đổi).
 *
 * <p>Cung cấp:
 * <ul>
 *   <li>{@link #isValidSkillId(Integer)} — kiểm tra skillId có trong single_skill.json không</li>
 *   <li>{@link #isValidTalentId(Integer)} — kiểm tra skillId có trong passive_skill.json không</li>
 *   <li>{@link #getSkillMaxLevel(Integer)} — lấy cấp tối đa cho một skill</li>
 *   <li>{@link #getTalentMaxLevel(Integer)} — lấy cấp tối đa cho một talent</li>
 *   <li>{@link #getDefaultSkillMaxLevel()} — fallback max level khi config chưa tải</li>
 * </ul>
 *
 * <p>Config properties:
 * <pre>
 *   skill.config.single-skill-path: gameworld/skill/single_skill.json
 *   skill.config.passive-skill-path: gameworld/skill/passive_skill.json
 *   skill.config.default-max-level: 20
 *   skill.config.refresh-interval-ms: 60000
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillConfigCache {


    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();

    // ─── Config props ────────────────────────────────────────────────────
    @Value("${skill.config.single-skill-path:gameworld/skill/single_skill.json}")
    private String singleSkillPath;

    @Value("${skill.config.passive-skill-path:gameworld/skill/passive_skill.json}")
    private String passiveSkillPath;

    @Value("${skill.config.default-max-level:20}")
    private int defaultMaxLevel;

    // ─── ETags ───────────────────────────────────────────────────────────
    private final AtomicReference<String> etagSingle  = new AtomicReference<>(null);
    private final AtomicReference<String> etagPassive = new AtomicReference<>(null);

    // ─── Data caches ─────────────────────────────────────────────────────
    /**
     * skill_id → max skill_level (từ single_skill.json).
     * ConcurrentHashMap để thread-safe read sau warm-up.
     */
    private final AtomicReference<Map<Integer, Integer>> singleSkillMaxLevels =
            new AtomicReference<>(Collections.emptyMap());

    /**
     * skill_id → max skill_level (từ passive_skill.json).
     */
    private final AtomicReference<Map<Integer, Integer>> passiveSkillMaxLevels =
            new AtomicReference<>(Collections.emptyMap());

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);

    // ─── Lifecycle ───────────────────────────────────────────────────────

    @EventListener(ContextRefreshedEvent.class)
    public void onContextReady() {
        if (warmedUp.compareAndSet(false, true)) {
            warmup();
        }
    }

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${skill.config.refresh-interval-ms:60000}")
    public void refresh() {
        refreshSingleSkill();
        refreshPassiveSkill();
    }

    private void warmup() {
        boolean ok1 = refreshSingleSkill();
        boolean ok2 = refreshPassiveSkill();
        if (ok1 && ok2) {
            log.info("[SkillConfigCache] Warmup OK — skill={} ids, talent={} ids",
                    singleSkillMaxLevels.get().size(), passiveSkillMaxLevels.get().size());
        } else {
            log.warn("[SkillConfigCache] Warmup partial (singleOk={}, passiveOk={}), will retry on schedule", ok1, ok2);
        }
    }

    // ─── Refreshers ──────────────────────────────────────────────────────

    private boolean refreshSingleSkill() {
        try {
            ResponseEntity<byte[]> res = configFeign.getFile(singleSkillPath, etagSingle.get());
            if (res.getStatusCode() == HttpStatus.NOT_MODIFIED) return true;
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                String body = new String(res.getBody(), StandardCharsets.UTF_8);
                Map<Integer, Integer> maxLevels = parseSingleSkill(body);
                singleSkillMaxLevels.set(maxLevels);
                updateEtag(res, etagSingle);
                log.info("[SkillConfigCache] single_skill.json loaded: {} skill entries, maxLevel examples={}",
                        maxLevels.size(), maxLevels.entrySet().stream().limit(3).toList());
                return true;
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) return true;
            log.warn("[SkillConfigCache] Cannot refresh single_skill.json (path={}): {}", singleSkillPath, ex.getMessage());
        } catch (Exception ex) {
            log.warn("[SkillConfigCache] Cannot refresh single_skill.json (path={}): {}", singleSkillPath, ex.getMessage());
        }
        return false;
    }

    private boolean refreshPassiveSkill() {
        try {
            ResponseEntity<byte[]> res = configFeign.getFile(passiveSkillPath, etagPassive.get());
            if (res.getStatusCode() == HttpStatus.NOT_MODIFIED) return true;
            if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                String body = new String(res.getBody(), StandardCharsets.UTF_8);
                Map<Integer, Integer> maxLevels = parsePassiveSkill(body);
                passiveSkillMaxLevels.set(maxLevels);
                updateEtag(res, etagPassive);
                log.info("[SkillConfigCache] passive_skill.json loaded: {} talent entries, maxLevel examples={}",
                        maxLevels.size(), maxLevels.entrySet().stream().limit(3).toList());
                return true;
            }
        } catch (FeignException ex) {
            if (ex.status() == 304) return true;
            log.warn("[SkillConfigCache] Cannot refresh passive_skill.json (path={}): {}", passiveSkillPath, ex.getMessage());
        } catch (Exception ex) {
            log.warn("[SkillConfigCache] Cannot refresh passive_skill.json (path={}): {}", passiveSkillPath, ex.getMessage());
        }
        return false;
    }

    // ─── Parsers ─────────────────────────────────────────────────────────

    /**
     * Parse {@code single_skill.json} → map skill_id → max skill_level.
     * Cấu trúc: {@code { "skill_cfg": [ { "skill_id":"1", "skill_level":"1", ... }, ... ] }}
     */
    private Map<Integer, Integer> parseSingleSkill(String json) {
        try {
            JsonNode root = om.readTree(json);
            JsonNode arr = root.get("skill_cfg");
            return parseMaxLevelMap(arr);
        } catch (Exception e) {
            log.warn("[SkillConfigCache] parseSingleSkill failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Parse {@code passive_skill.json} → map skill_id → max skill_level.
     * Cấu trúc: {@code { "passive_cfg": [ { "skill_id":"1", "skill_level":"1", ... }, ... ] }}
     */
    private Map<Integer, Integer> parsePassiveSkill(String json) {
        try {
            JsonNode root = om.readTree(json);
            JsonNode arr = root.get("passive_cfg");
            return parseMaxLevelMap(arr);
        } catch (Exception e) {
            log.warn("[SkillConfigCache] parsePassiveSkill failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Duyệt một mảng JSON, group theo {@code skill_id}, lấy {@code max(skill_level)}.
     * Hỗ trợ cả string và int cho các trường này.
     */
    private Map<Integer, Integer> parseMaxLevelMap(JsonNode arr) {
        if (arr == null || !arr.isArray()) return Collections.emptyMap();
        Map<Integer, Integer> result = new ConcurrentHashMap<>();
        for (JsonNode node : arr) {
            Integer skillId = parseIntField(node, "skill_id");
            Integer skillLevel = parseIntField(node, "skill_level");
            if (skillId == null || skillId <= 0 || skillLevel == null || skillLevel <= 0) continue;
            result.merge(skillId, skillLevel, Math::max);
        }
        return Collections.unmodifiableMap(result);
    }

    // ─── Public API ──────────────────────────────────────────────────────

    /**
     * Kiểm tra xem skillId có tồn tại trong single_skill.json không.
     *
     * <p>Nếu config chưa load (rỗng), trả {@code true} để không block thao tác
     * (fail-open khi config-service chưa sẵn sàng).
     */
    public boolean isValidSkillId(Integer skillId) {
        if (skillId == null || skillId <= 0) return false;
        Map<Integer, Integer> map = singleSkillMaxLevels.get();
        if (map.isEmpty()) return true; // fail-open: config chưa load
        return map.containsKey(skillId);
    }

    /**
     * Kiểm tra xem skillId có tồn tại trong passive_skill.json không.
     *
     * <p>Tương tự {@link #isValidSkillId}, fail-open nếu config rỗng.
     */
    public boolean isValidTalentId(Integer skillId) {
        if (skillId == null || skillId <= 0) return false;
        Map<Integer, Integer> map = passiveSkillMaxLevels.get();
        if (map.isEmpty()) return true; // fail-open
        return map.containsKey(skillId);
    }

    /**
     * Lấy cấp tối đa cho một kỹ năng cụ thể.
     * Trả {@link #defaultMaxLevel} nếu config chưa tải hoặc skill không có trong config.
     */
    public int getSkillMaxLevel(Integer skillId) {
        if (skillId == null || skillId <= 0) return defaultMaxLevel;
        Map<Integer, Integer> map = singleSkillMaxLevels.get();
        if (map.isEmpty()) return defaultMaxLevel;
        return map.getOrDefault(skillId, defaultMaxLevel);
    }

    /**
     * Lấy cấp tối đa cho một thiên phú kỹ năng cụ thể.
     */
    public int getTalentMaxLevel(Integer skillId) {
        if (skillId == null || skillId <= 0) return defaultMaxLevel;
        Map<Integer, Integer> map = passiveSkillMaxLevels.get();
        if (map.isEmpty()) return defaultMaxLevel;
        return map.getOrDefault(skillId, defaultMaxLevel);
    }

    /**
     * Lấy cấp tối đa mặc định (fallback toàn cục).
     * Bằng giá trị property {@code skill.config.default-max-level} (default: 20).
     */
    public int getDefaultSkillMaxLevel() {
        return defaultMaxLevel;
    }

    /**
     * Trả về set tất cả skillId hợp lệ trong single_skill.json (để diagnostics).
     */
    public Set<Integer> getAllValidSkillIds() {
        return singleSkillMaxLevels.get().keySet();
    }

    /**
     * Trả về set tất cả talentId hợp lệ trong passive_skill.json (để diagnostics).
     */
    public Set<Integer> getAllValidTalentIds() {
        return passiveSkillMaxLevels.get().keySet();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Integer parseIntField(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        if (n.isInt() || n.isLong()) return n.intValue();
        try { return Integer.valueOf(n.asText().trim()); } catch (Exception e) { return null; }
    }

    private void updateEtag(ResponseEntity<byte[]> res, AtomicReference<String> etagRef) {
        String et = res.getHeaders().getFirst(HttpHeaders.ETAG);
        if (StringUtils.hasText(et)) etagRef.set(et);
    }
}

