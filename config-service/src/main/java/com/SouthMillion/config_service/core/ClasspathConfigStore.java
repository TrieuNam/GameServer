package com.SouthMillion.config_service.core;

import org.SouthMillion.dto.config.ConfigFileData;
import org.SouthMillion.dto.config.Hashing;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClasspathConfigStore implements ConfigStore {
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private volatile String revision = "init";
    // indexes
    private final Map<String, Resource> byRelPath = new ConcurrentHashMap<>();
    private final List<String> items = new ArrayList<>();
    private final List<String> logic = new ArrayList<>();
    private final List<String> drops = new ArrayList<>();
    private final List<String> global = new ArrayList<>();
    private final List<String> skill = new ArrayList<>();
    private final List<String> monster = new ArrayList<>();
    private final List<String> server = new ArrayList<>();

    public ClasspathConfigStore() { reload(); }

    @Override public void reload() {
        try {
            byRelPath.clear(); items.clear(); logic.clear(); drops.clear();
            global.clear(); skill.clear(); monster.clear(); server.clear();

            // scan all resources under classpath:config/**
            var all = Arrays.asList(resolver.getResources("classpath*:config/**"));
            // keep only readable files
            for (Resource r : all) {
                if (!r.isReadable()) continue;
                var url = r.getURL().toString();
                int idx = url.indexOf("/config/");
                if (idx < 0) continue;
                var rel = url.substring(url.indexOf("config/") + "config/".length()); // relative inside config/
                if (rel.endsWith("/")) continue; // folder
                byRelPath.put(rel, r);

                // bucketize
                if (rel.startsWith("gameworld/item/") && rel.endsWith(".json")) {
                    items.add(rel.substring("gameworld/item/".length(), rel.length()-".json".length()));
                } else if (rel.startsWith("gameworld/logicconfig/") && rel.endsWith(".json")) {
                    logic.add(rel.substring("gameworld/logicconfig/".length(), rel.length()-".json".length())
                            .replace('\\','/'));
                } else if (rel.startsWith("gameworld/drop/") && rel.endsWith(".xml")) {
                    drops.add(rel.substring("gameworld/drop/".length(), rel.length()-".xml".length()));
                } else if (rel.startsWith("gameworld/globalconfig/") && rel.endsWith(".json")) {
                    global.add(rel.substring("gameworld/globalconfig/".length(), rel.length()-".json".length()));
                } else if (rel.startsWith("gameworld/skill/") && rel.endsWith(".json")) {
                    skill.add(rel.substring("gameworld/skill/".length(), rel.length()-".json".length()));
                } else if (rel.startsWith("gameworld/monster/") && rel.endsWith(".json")) {
                    monster.add(rel.substring("gameworld/monster/".length(), rel.length()-".json".length()));
                } else if (rel.startsWith("serverconfig/") && (rel.endsWith(".json")||rel.endsWith(".xml")||rel.endsWith(".txt"))) {
                    String base = rel.substring("serverconfig/".length());
                    int dot = base.lastIndexOf('.');
                    if (dot>0) base = base.substring(0,dot);
                    server.add(base);
                }
            }
            // sort & unique
            List<List<String>> lists = List.of(items, logic, drops, global, skill, monster, server);
            for (var l: lists) {
                var set = new TreeSet<>(l);
                l.clear(); l.addAll(set);
            }
            // compute revision = sha1 of (paths + sizes)
            var sb = new StringBuilder();
            byRelPath.forEach((k,v)-> {
                try { sb.append(k).append(':').append(v.contentLength()).append('\n'); } catch(Exception ignored){}
            });
            revision = Hashing.sha1(sb.toString().getBytes(StandardCharsets.UTF_8)).replace("\"",""); // plain
        } catch (IOException e) {
            throw new RuntimeException("Scan classpath resources failed", e);
        }
    }

    @Override
    public Optional<ConfigFileData> getFileByKey(String pathKey) {
        try {
            String rel = mapKeyToRelPath(pathKey);
            return getByRelativePath(rel);
        } catch (Exception e) {           // bắt rộng để không bao giờ 500 vì bug mapping
            return Optional.empty();
        }
    }

    @Override public Optional<ConfigFileData> getByRelativePath(String rel) {
        Resource r = byRelPath.get(rel);
        if (r==null || !r.isReadable()) return Optional.empty();
        try {
            byte[] bytes = r.getInputStream().readAllBytes();
            long lm = Instant.now().getEpochSecond(); // classpath resource không có mtime tin cậy
            String ct = guessContentType(rel);
            String etag = Hashing.sha1((revision+"|"+rel).getBytes(StandardCharsets.UTF_8), bytes);
            String key = relToKey(rel);
            return Optional.of(new ConfigFileData(key, revision, lm, etag, ct, bytes));
        } catch (IOException e) { return Optional.empty(); }
    }

    @Override public String currentRevision() { return revision; }

    // === listing
    @Override public List<String> listItems() { return items; }
    @Override public List<String> listLogic() { return logic; }
    @Override public List<String> listDrops() { return drops; }
    @Override public List<String> listGlobal(){ return global; }
    @Override public List<String> listSkill(){ return skill; }
    @Override public List<String> listMonster(){ return monster; }
    @Override public List<String> listServerConfig(){ return server; }

    // === helpers
    private static String guessContentType(String rel){
        if (rel.endsWith(".json")) return "application/json";
        if (rel.endsWith(".xml"))  return "application/xml";
        if (rel.endsWith(".txt"))  return "text/plain";
        return "application/octet-stream";
    }
    private static String relToKey(String rel){
        // unify to "config/<rel>"
        return "config/" + rel.replace('\\','/');
    }
    private static boolean hasExt(String p) {
        int i = p.lastIndexOf('.');
        return i > p.lastIndexOf('/');    // có dấu chấm sau dấu slash cuối cùng -> có ext
    }
    private static String ensureExt(String path, String ext) {
        return path.endsWith(ext) ? path : path + ext;
    }

    private String mapKeyToRelPath(String key){
        if (key == null) throw new IllegalArgumentException("key is null");
        String s = key.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("key is blank");
        if (s.startsWith("/")) s = s.substring(1);

        // Đảm bảo có prefix "config/"
        if (!s.startsWith("config/")) {
            s = "config/" + s;
        }

        // Từ đây luôn an toàn
        String rest = s.substring("config/".length()); // ví dụ: "gameworld/item/block_item"

        // Tách domain/type/leaf: "gameworld/item/<leaf>"
        String[] parts = rest.split("/", 3);
        if (parts.length < 2) throw new IllegalArgumentException("bad key: " + key);

        String domain = parts[0];                // gameworld | serverconfig | ...
        String type   = parts[1];                // item|logicconfig|drop|global|skill|monster|file|...
        String leaf   = (parts.length >= 3) ? parts[2] : "";

        if ("gameworld".equals(domain)) {
            switch (type) {
                case "item":        return ensureExt("gameworld/item/"         + leaf, ".json");
                case "logicconfig": return ensureExt("gameworld/logicconfig/"  + leaf, ".json");
                case "drop":        return ensureExt("gameworld/drop/"         + leaf, ".xml");
                case "global":      return ensureExt("gameworld/globalconfig/" + leaf, ".json");
                case "skill":       return ensureExt("gameworld/skill/"        + leaf, ".json");
                case "monster":     return ensureExt("gameworld/monster/"      + leaf, ".json");
                case "file":        return "gameworld/" + leaf; // leaf đã kèm ext
                default:
                    throw new IllegalArgumentException("unsupported type: " + type);
            }
        } else if ("serverconfig".equals(domain)) {
            // Cho phép có/không có extension
            String base = "serverconfig/" + leaf;
            if (hasExt(leaf)) {
                if (byRelPath.containsKey(base)) return base;
            } else {
                if (byRelPath.containsKey(base + ".json")) return base + ".json";
                if (byRelPath.containsKey(base + ".xml"))  return base + ".xml";
                if (byRelPath.containsKey(base + ".txt"))  return base + ".txt";
            }
            throw new IllegalArgumentException("not found: " + base);
        } else {
            // (1) Cho phép raw relative dưới "config/" nếu tồn tại
            if (byRelPath.containsKey(rest)) return rest;

            // (2) Hỗ trợ shorthand "logicconfig/..." cũ
            if (rest.startsWith("logicconfig/")) {
                String candidate = ensureExt(rest, ".json");
                if (byRelPath.containsKey(candidate)) return candidate;
            }

            // (3) Nếu caller lỡ đưa kiểu "config/block_item" → coi như leaf item rút gọn
            if (!rest.contains("/")) {
                String candidate = ensureExt("gameworld/item/" + rest, ".json");
                if (byRelPath.containsKey(candidate)) return candidate;
            }

            throw new IllegalArgumentException("unsupported domain: " + domain);
        }
    }
}