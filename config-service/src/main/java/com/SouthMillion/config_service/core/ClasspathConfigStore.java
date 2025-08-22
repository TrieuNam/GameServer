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

    @Override public Optional<ConfigFileData> getFileByKey(String pathKey) {
        try {
            String rel = mapKeyToRelPath(pathKey);
            return getByRelativePath(rel);
        } catch (IllegalArgumentException e) {
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
    private String mapKeyToRelPath(String key){
        String s= null;
        if (!key.startsWith("config/")) {
             s = "config/" + key;
        }
        String rest = s.substring("config/".length());
        String[] parts = rest.split("/", 3); // e.g. gameworld/item/<leaf>
        if (parts.length < 2) throw new IllegalArgumentException("bad key");

        String domain = parts[0]; // gameworld | serverconfig | ...
        String type   = parts[1]; // item|logic|drop|global|skill|monster|file
        String leaf   = parts.length>=3 ? parts[2] : "";

        if ("gameworld".equals(domain)) {
            switch (type) {
                case "item":   return "gameworld/item/" + leaf + ".json";
                case "logicconfig":  return "gameworld/logicconfig/" + leaf + ".json"; // leaf có thể chứa "randactivity/xxx"
                case "drop":   return "gameworld/drop/" + leaf + ".xml";
                case "global": return "gameworld/globalconfig/" + leaf + ".json";
                case "skill":  return "gameworld/skill/" + leaf + ".json";
                case "monster":return "gameworld/monster/" + leaf + ".json";
                case "file":   return "gameworld/" + leaf; // leaf kèm extension
                default: throw new IllegalArgumentException("unsupported type: "+type);
            }
        } else if ("serverconfig".equals(domain)) {
            // thử theo thứ tự json->xml->txt
            String base = "serverconfig/" + leaf;
            if (byRelPath.containsKey(base + ".json")) return base + ".json";
            if (byRelPath.containsKey(base + ".xml"))  return base + ".xml";
            if (byRelPath.containsKey(base + ".txt"))  return base + ".txt";
            // nếu leaf đã có extension, trả thẳng
            if (byRelPath.containsKey(base)) return base;
            throw new IllegalArgumentException("not found: "+base);
        } else {
            // cho phép key "config/<any rel>" đã có ext
            if (byRelPath.containsKey(rest)) return rest;
            throw new IllegalArgumentException("unsupported domain: "+domain);
        }
    }
}