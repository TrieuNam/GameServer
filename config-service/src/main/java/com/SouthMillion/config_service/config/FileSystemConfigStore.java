package com.SouthMillion.config_service.config;

import com.SouthMillion.config_service.core.ConfigStore;
import org.SouthMillion.dto.config.ConfigFileData;
import org.SouthMillion.dto.config.Hashing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileSystemConfigStore implements ConfigStore {
    private final Path root;
    private volatile String rev = "init";
    private final Map<String, Path> byRelPath = new HashMap<>();
    private final List<String> items = new ArrayList<>(), logic = new ArrayList<>(), drops = new ArrayList<>(),
            global = new ArrayList<>(), skill = new ArrayList<>(), monster = new ArrayList<>(), server = new ArrayList<>();

    public FileSystemConfigStore(Path configDir) {
        this.root = configDir.toAbsolutePath().normalize();
        reload();
    }

    @Override
    public void reload() {
        try {
            byRelPath.clear();
            items.clear();
            logic.clear();
            drops.clear();
            global.clear();
            skill.clear();
            monster.clear();
            server.clear();
            // walk root
            try (var s = Files.walk(root)) {
                s.filter(Files::isRegularFile).forEach(p -> {
                    String rel = root.relativize(p).toString().replace('\\', '/');
                    byRelPath.put(rel, p);
                    if (rel.startsWith("gameworld/item/") && rel.endsWith(".json"))
                        items.add(rel.substring(16, rel.length() - 5));
                    else if (rel.startsWith("gameworld/logicconfig/") && rel.endsWith(".json"))
                        logic.add(rel.substring(23, rel.length() - 5));
                    else if (rel.startsWith("gameworld/drop/") && rel.endsWith(".xml"))
                        drops.add(rel.substring(16, rel.length() - 4));
                    else if (rel.startsWith("gameworld/globalconfig/") && rel.endsWith(".json"))
                        global.add(rel.substring(23, rel.length() - 5));
                    else if (rel.startsWith("gameworld/skill/") && rel.endsWith(".json"))
                        skill.add(rel.substring(17, rel.length() - 5));
                    else if (rel.startsWith("gameworld/monster/") && rel.endsWith(".json"))
                        monster.add(rel.substring(19, rel.length() - 5));
                    else if (rel.startsWith("serverconfig/") && (rel.endsWith(".json") || rel.endsWith(".xml") || rel.endsWith(".txt"))) {
                        String base = rel.substring("serverconfig/".length());
                        int dot = base.lastIndexOf('.');
                        if (dot > 0) base = base.substring(0, dot);
                        server.add(base);
                    }
                });
            }
            // compute revision
            var sb = new StringBuilder();
            for (var e : byRelPath.entrySet()) {
                try {
                    sb.append(e.getKey()).append(':').append(Files.size(e.getValue())).append('\n');
                } catch (Exception ignored) {
                }
            }
            rev = Hashing.sha1(sb.toString().getBytes()).replace("\"", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ConfigFileData> getFileByKey(String key) {
        try {
            return getByRelativePath(mapKeyToRelPath(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ConfigFileData> getByRelativePath(String rel) {
        Path p = byRelPath.get(rel);
        if (p == null) return Optional.empty();
        try {
            byte[] bytes = Files.readAllBytes(p);
            long lm = Files.getLastModifiedTime(p).toMillis() / 1000;
            String ct = rel.endsWith(".json") ? "application/json" : rel.endsWith(".xml") ? "application/xml" :
                    rel.endsWith(".txt") ? "text/plain" : "application/octet-stream";
            String etag = Hashing.sha1((rev + "|" + rel).getBytes(), bytes);
            return Optional.of(new ConfigFileData("config/" + rel, rev, lm, etag, ct, bytes));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public String currentRevision() {
        return rev;
    }

    @Override
    public List<String> listItems() {
        return items;
    }

    @Override
    public List<String> listLogic() {
        return logic;
    }

    @Override
    public List<String> listDrops() {
        return drops;
    }

    @Override
    public List<String> listGlobal() {
        return global;
    }

    @Override
    public List<String> listSkill() {
        return skill;
    }

    @Override
    public List<String> listMonster() {
        return monster;
    }

    @Override
    public List<String> listServerConfig() {
        return server;
    }

    private String mapKeyToRelPath(String key) {
        if (!key.startsWith("config/")) throw new IllegalArgumentException("bad key");
        String rest = key.substring(7);
        String[] parts = rest.split("/", 3);
        if (parts.length < 2) throw new IllegalArgumentException("bad key");
        String domain = parts[0], type = parts[1], leaf = parts.length >= 3 ? parts[2] : "";
        if ("gameworld".equals(domain)) {
            return switch (type) {
                case "item" -> "gameworld/item/" + leaf + ".json";
                case "logic" -> "gameworld/logicconfig/" + leaf + ".json";
                case "drop" -> "gameworld/drop/" + leaf + ".xml";
                case "globalconfig" -> "gameworld/globalconfig/" + leaf + ".json";
                case "skill" -> "gameworld/skill/" + leaf + ".json";
                case "monster" -> "gameworld/monster/" + leaf + ".json";
                case "file" -> "gameworld/" + leaf;
                default -> throw new IllegalArgumentException("unsupported " + type);
            };
        } else if ("serverconfig".equals(domain)) {
            String base = "serverconfig/" + leaf;
            if (byRelPath.containsKey(base + ".json")) return base + ".json";
            if (byRelPath.containsKey(base + ".xml")) return base + ".xml";
            if (byRelPath.containsKey(base + ".txt")) return base + ".txt";
            if (byRelPath.containsKey(base)) return base;
            throw new IllegalArgumentException("not found");
        } else {
            if (byRelPath.containsKey(rest)) return rest;
            throw new IllegalArgumentException("unsupported");
        }
    }
}