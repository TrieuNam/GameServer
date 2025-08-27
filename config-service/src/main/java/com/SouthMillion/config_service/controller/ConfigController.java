package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.config.ConfigProperties;
import com.SouthMillion.config_service.core.ConfigStore;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.SouthMillion.dto.config.ConfigFileData;
import org.SouthMillion.dto.config.Hashing;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quy ước KEY:
 *  - Mọi file trong store đều được tham chiếu bằng key tuyệt đối bắt đầu với "config/..."
 *    ví dụ: config/gameworld/logicconfig/roleexp.json
 *
 * L1 Cache:
 *  - Đối với lookup bằng KEY tuyệt đối: dùng key cache = key + "@" + currentRevision
 *  - Đối với lookup bằng relative path (BY:): dùng "BY:" + rel + "@" + currentRevision
 *    để tránh dữ liệu cũ khi revision thay đổi.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/config")
public class ConfigController {
    private final ConfigStore store;
    private final ConfigProperties props;
    private final Cache<String, ConfigFileData> l1;

    public ConfigController(ConfigStore store, ConfigProperties props, Cache<String, ConfigFileData> l1) {
        this.store = store;
        this.props = props;
        this.l1 = l1;
    }

    // ============================================================
    // ======================= Helpers =============================
    // ============================================================

    /**
     * Tạo KEY tuyệt đối trong store (không có slash ở đầu).
     * Ví dụ: k("logicconfig", "roleexp.json") => "config/gameworld/logicconfig/roleexp.json"
     */
    private static String k(String folder, String leaf) {
        String cleanLeaf = leaf.replaceAll("^/+", "");
        return "gameworld/" + folder + "/" + cleanLeaf;
    }

    /**
     * Chuẩn hóa relative path cho "/by-path" và "/{name}.json".
     * Không cho phép ".." để tránh traversal.
     */
    private static String normalizeRel(String rel) {
        String r = rel.replace("\\", "/").replaceAll("^/+", "");
        if (r.contains("..")) throw new IllegalArgumentException("invalid path");
        return r;
    }

    /**
     * Lấy từ L1 cache (key tuyệt đối) hoặc store. Key phải bắt đầu bằng "config/".
     * Trả về Pair(data, fromCache) để gắn header X-Cache.
     */
    private Optional<AbstractMap.SimpleEntry<ConfigFileData, Boolean>> cachedHit(String absKey) {
        String cacheKey = absKey + "@" + store.currentRevision();
        ConfigFileData d = l1.get(cacheKey, _k -> store.getFileByKey(absKey).orElse(null));

        if (d == null) {
            return store.getFileByKey(absKey).map(e -> new AbstractMap.SimpleEntry<>(e, false));
        }
        boolean hit = l1.asMap().containsKey(cacheKey);
        return Optional.of(new AbstractMap.SimpleEntry<>(d, hit));
    }

    /**
     * Lấy theo relative path (ví dụ "config/logicconfig/xxx.json" hoặc "gameworld/logicconfig/xxx.json").
     * rel không được bắt đầu bởi '/', và KHÔNG kèm "config/" ở đầu nếu sẽ gọi getByRelativePath.
     */
    private Optional<AbstractMap.SimpleEntry<ConfigFileData, Boolean>> cachedByRel(String relUnderConfig) {
        String rel = normalizeRel(relUnderConfig); // ví dụ: "logicconfig/roleexp.json"
        if (!props.getCache().isL1Enabled()) {
            return store.getByRelativePath(rel).map(d -> new AbstractMap.SimpleEntry<>(d, false));
        }
        String cacheKey = "BY:" + rel + "@" + store.currentRevision();
        ConfigFileData d = l1.get(cacheKey, _k -> store.getByRelativePath(rel).orElse(null));
        if (d == null) return Optional.empty();
        boolean hit = l1.asMap().containsKey(cacheKey);
        return Optional.of(new AbstractMap.SimpleEntry<>(d, hit));
    }

    /**
     * Trả file với hỗ trợ conditional GET. Nếu force200=true thì luôn trả 200.
     * Gắn kèm header X-Cache để debug (HIT/MISS).
     */
    private ResponseEntity<byte[]> serve(ConfigFileData f, HttpServletRequest req, boolean force200, boolean cacheHit) {
        if (!force200) {
            // ETag
            String inm = req.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (inm != null && !inm.isBlank()) {
                String etag = f.etag();
                for (String part : inm.split(",")) {
                    String t = part.trim();
                    if (t.equals(etag) || t.equals("\"" + etag + "\"") || t.equals("W/\"" + etag + "\"")) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                                .eTag(etag)
                                .lastModified(f.lastModifiedEpoch() * 1000)
                                .header("X-Cache", cacheHit ? "HIT" : "MISS")
                                .build();
                    }
                }
            }
            // Last-Modified
            long ims = req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
            if (ims > 0 && (f.lastModifiedEpoch() * 1000) <= ims) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(f.etag())
                        .lastModified(f.lastModifiedEpoch() * 1000)
                        .header("X-Cache", cacheHit ? "HIT" : "MISS")
                        .build();
            }
        }
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType(f.contentType()));
        h.setETag(f.etag());
        h.setLastModified(f.lastModifiedEpoch() * 1000);
        h.add("X-Config-Revision", f.revision());
        h.add("X-Cache", cacheHit ? "HIT" : "MISS");
        h.setCacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(60)).cachePublic());
        return new ResponseEntity<>(f.content(), h, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> serve(ConfigFileData f, HttpServletRequest req, boolean force200) {
        return serve(f, req, force200, /*cacheHit*/ true);
    }

    private ResponseEntity<byte[]> serve(ConfigFileData f, HttpServletRequest req) {
        return serve(f, req, false, /*cacheHit*/ true);
    }

    // ============================================================
    // =================== Version & reload =======================
    // ============================================================

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "revision", store.currentRevision(),
                "ts", Instant.now().getEpochSecond(),
                "mode", props.getMode()
        );
    }

    @PostMapping("/internal/reload")
    public Map<String, Object> reload() {
        store.reload();
        l1.invalidateAll();
        return Map.of("ok", true, "revision", store.currentRevision());
    }

    // ============================================================
    // ====================== BASIC GET ===========================
    // ============================================================

    @GetMapping("/gameworld/item/{name}")
    public ResponseEntity<byte[]> getItem(@PathVariable @NotBlank String name, HttpServletRequest req) {
        String key = "config/gameworld/item/" + name.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Hỗ trợ cả logic/randactivity/xxx qua {*feature}
    @GetMapping("/gameworld/logic/{*feature}")
    public ResponseEntity<byte[]> getLogic(@PathVariable("feature") String feature, HttpServletRequest req) {
        String leaf = feature.replaceAll("^/+", "");
        String key = "config/gameworld/logic/" + leaf;
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/drop/{id}")
    public ResponseEntity<byte[]> drop(@PathVariable String id, HttpServletRequest req) {
        String key = "config/gameworld/drop/" + id.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/global/{name}")
    public ResponseEntity<byte[]> global(@PathVariable String name, HttpServletRequest req) {
        String key = "config/gameworld/global/" + name.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/skill/{name}")
    public ResponseEntity<byte[]> skill(@PathVariable String name, HttpServletRequest req) {
        String key = "config/gameworld/skill/" + name.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/monster/{name}")
    public ResponseEntity<byte[]> monster(@PathVariable String name, HttpServletRequest req) {
        String key = "config/gameworld/monster/" + name.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/serverconfig/{name}")
    public ResponseEntity<byte[]> serverconfig(@PathVariable String name, HttpServletRequest req) {
        String key = "config/serverconfig/" + name.replaceAll("^/+", "");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // by relative path under config/ (vd ?p=gameworld/battlemonstermanager.xml)
    @GetMapping("/by-path")
    public ResponseEntity<byte[]> byPath(@RequestParam("p") String rel,
                                         @RequestParam(value = "force", defaultValue = "0") int force,
                                         HttpServletRequest req) {
        // Ở store, getByRelativePath nhận REL tính từ "config/" => ví dụ "gameworld/logic/xxx.json"
        String normalized = normalizeRel(rel);
        return cachedByRel(normalized)
                .map(e -> serve(e.getKey(), req, force == 1, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ============================================================
    // ======================= Bundle/Index ========================
    // ============================================================

    @GetMapping("/bundle")
    public ResponseEntity<List<ConfigEnvelope<String>>> bundle(
            @RequestParam MultiValueMap<String, String> params) {

        List<String> keys = new ArrayList<>(params.getOrDefault("keys", List.of()));
        if (keys.size() == 1 && keys.get(0) != null && keys.get(0).contains(",")) {
            keys = Arrays.stream(keys.get(0).split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .toList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String k : keys) {
            String absKey = normalizeAbsKey(k);
            if (absKey != null) normalized.add(absKey);
        }

        var list = normalized.stream()
                .map(this::cachedHit)           // Optional<SimpleEntry<ConfigFileData, Boolean>>
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(e -> {
                    var f = e.getKey();
                    String payload = switch (f.contentType()) {
                        case "application/json", "application/xml", "text/plain" ->
                                new String(f.content(), java.nio.charset.StandardCharsets.UTF_8);
                        default -> java.util.Base64.getEncoder().encodeToString(f.content());
                    };
                    return new ConfigEnvelope<>(f.key(), f.revision(), f.lastModifiedEpoch(), f.etag(), payload);
                })
                .toList();

        return ResponseEntity.ok(list);
    }

    // HỢP NHẤT QUY TẮC CHUẨN HOÁ VỚI CLIENT
    private static String normalizeAbsKey(String k) {
        if (k == null) return null;
        String v = k.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("/")) v = v.substring(1);

        if (v.startsWith("config/"))   return v;                 // đã tuyệt đối
        if (v.startsWith("logicconfig/")) return "config/" + v;  // shorthand logicconfig
        // còn lại -> coi là leaf của item
        return "config/gameworld/item/" + v;
    }
    @GetMapping("/index")
    public Map<String, Object> index() {
        return Map.of(
                "item", Map.of("count", store.listItems().size()),
                "logic", Map.of("count", store.listLogic().size()),
                "drop", Map.of("count", store.listDrops().size()),
                "global", Map.of("count", store.listGlobal().size()),
                "skill", Map.of("count", store.listSkill().size()),
                "monster", Map.of("count", store.listMonster().size()),
                "serverconfig", Map.of("count", store.listServerConfig().size())
        );
    }

    @GetMapping("/list/{cat}")
    public List<String> list(@PathVariable String cat,
                             @RequestParam(defaultValue = "0") int offset,
                             @RequestParam(defaultValue = "200") int limit) {
        List<String> all = switch (cat) {
            case "item" -> store.listItems();
            case "logic" -> store.listLogic();
            case "drop" -> store.listDrops();
            case "global" -> store.listGlobal();
            case "skill" -> store.listSkill();
            case "monster" -> store.listMonster();
            case "serverconfig" -> store.listServerConfig();
            default -> List.of();
        };
        int from = Math.min(all.size(), Math.max(0, offset));
        int to = Math.min(all.size(), from + Math.max(0, limit));
        return all.subList(from, to);
    }

    // ============================================================
    // ==============  Direct JSON by name (force)  ===============
    // ============================================================
    /**
     * Map tới file: config/{name}.json (tức REL = "{name}.json")
     * Ví dụ: GET /config/roleexp.json => lấy "config/roleexp.json"
     * Nếu bạn muốn tới logicconfig: dùng /config/gameworld/logicconfig/roleexp.json (các API phía dưới).
     */
    @GetMapping("/{name}.json")
    public ResponseEntity<byte[]> configJsonByName(@PathVariable String name,
                                                   @RequestParam(value = "force", defaultValue = "0") int force,
                                                   HttpServletRequest req) {
        String rel = normalizeRel("config/"+name + ".json");          // ví dụ "roleexp.json"
        return cachedByRel(rel)                              // => getByRelativePath("roleexp.json")
                .map(e -> serve(e.getKey(), req, force == 1, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ============================================================
    // ============  Override / Reload / Purge (BY:)  =============
    // ============================================================

    @PostMapping(path = "/{name}.json/override", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> overrideConfigJson(
            @PathVariable String name,
            @RequestBody byte[] body,
            @RequestHeader(value = "If-Match", required = false) String ifMatch
    ) {
        String rel = normalizeRel(name + ".json"); // relative trong store
        String l1Key = "BY:" + rel + "@" + store.currentRevision();

        var current = l1.getIfPresent(l1Key);
        if (ifMatch != null && current != null && !ifMatch.equals(current.etag())) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of("ok", false, "reason", "etag-mismatch", "currentEtag", current.etag()));
        }

        long nowSec = System.currentTimeMillis() / 1000;
        String revision = store.currentRevision() + "+hot";
        // etag = sha1(rev|rel, content)
        String etag = Hashing.sha1((revision + "|" + rel).getBytes(), body);

        // Key tuyệt đối trong file data luôn là "config/" + rel
        var cfg = new ConfigFileData("config/" + rel, revision, nowSec,
                etag, "application/json", body);

        l1.put(l1Key, cfg);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "path", rel,
                "etag", etag,
                "lastModified", nowSec,
                "revision", revision,
                "size", body.length
        ));
    }

    @RequestMapping(value = "/{name}.json/reload", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Map<String, Object>> reloadConfigJsonByName(@PathVariable String name) {
        String rel = normalizeRel(name + ".json");
        String l1Key = "BY:" + rel + "@" + store.currentRevision();

        var before = l1.getIfPresent(l1Key);
        l1.invalidate(l1Key);

        var refreshed = store.getByRelativePath(rel).orElse(null);
        if (refreshed != null) {
            l1.put(l1Key, refreshed);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ok", refreshed != null);
        body.put("path", rel);
        if (before != null) {
            body.put("prevEtag", before.etag());
            body.put("prevLastModified", before.lastModifiedEpoch());
        }
        if (refreshed != null) {
            body.put("etag", refreshed.etag());
            body.put("lastModified", refreshed.lastModifiedEpoch());
            body.put("revision", refreshed.revision());
            body.put("size", refreshed.content().length);
        } else {
            body.put("reason", "not-found");
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{name}.json/purge")
    public ResponseEntity<Map<String, Object>> purgeConfigJsonByName(@PathVariable String name) {
        String rel = normalizeRel(name + ".json");
        String l1Key = "BY:" + rel + "@" + store.currentRevision();
        boolean existed = l1.getIfPresent(l1Key) != null;
        l1.invalidate(l1Key);
        return ResponseEntity.ok(Map.of("ok", true, "path", rel, "existed", existed));
    }

    // ============================================================
    // ==========  NEW: logicconfig & globalconfig  ===============
    // ============================================================

    // --- logicconfig (3 file cụ thể)
    @GetMapping("/gameworld/logicconfig/roleexp.json")
    public ResponseEntity<byte[]> logicRoleExp(HttpServletRequest req) {
        String key = k("logicconfig", "roleexp"); // => "config/gameworld/logicconfig/roleexp.json"
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/logicconfig/role_name.json")
    public ResponseEntity<byte[]> logicRoleName(HttpServletRequest req) {
        String key = k("logicconfig", "role_name");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/logicconfig/bag_cfg.json")
    public ResponseEntity<byte[]> logicBagCfg(HttpServletRequest req) {
        String key = k("logicconfig", "bag_cfg");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Wildcard cho các file khác trong logicconfig
    @GetMapping("/gameworld/logicconfig/{*path}")
    public ResponseEntity<byte[]> logicConfigAny(@PathVariable("path") String path, HttpServletRequest req) {
        String key = k("logicconfig", path);
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- globalconfig (2 file cụ thể)
    @GetMapping("/gameworld/globalconfig/keyconfig.json")
    public ResponseEntity<byte[]> globalKeyConfig(HttpServletRequest req) {
        String key = k("globalconfig", "keyconfig");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/gameworld/globalconfig/otherconfig.json")
    public ResponseEntity<byte[]> globalOtherConfig(HttpServletRequest req) {
        String key = k("globalconfig", "otherconfig");
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Wildcard cho các file khác trong globalconfig
    @GetMapping("/gameworld/globalconfig/{*path}")
    public ResponseEntity<byte[]> globalConfigAny(@PathVariable("path") String path, HttpServletRequest req) {
        String key = k("globalconfig", path);
        return cachedHit(key).map(e -> serve(e.getKey(), req, false, e.getValue()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}