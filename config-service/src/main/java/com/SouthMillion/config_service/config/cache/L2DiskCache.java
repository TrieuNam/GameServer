package com.SouthMillion.config_service.config.cache;


import com.SouthMillion.config_service.config.ConfigProps;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class L2DiskCache implements CacheTier {

    private final boolean enabled;
    private final Path root;
    private final long maxBytesPerEntry;
    private final ObjectMapper om;

    public L2DiskCache(ConfigProps props, ObjectMapper om) {
        var l2 = props.l2();
        this.enabled = l2 != null && Boolean.TRUE.equals(l2.enabled());
        this.root = toRoot(l2);
        this.maxBytesPerEntry = (l2 != null && l2.maxBytesPerEntry() != null)
                ? l2.maxBytesPerEntry() : 10 * 1024 * 1024; // 10MB
        this.om = om.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (enabled) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create L2 root dir: " + root, e);
            }
        }
    }

    @Override
    public Optional<ConfigEnvelope> get(String path) {
        if (!enabled || path == null || path.isBlank()) return Optional.empty();
        var dir = entryDir(path);
        var meta = dir.resolve("meta.json");
        var data = dir.resolve("data.bin");

        try {
            if (!Files.isRegularFile(meta) || !Files.isRegularFile(data)) return Optional.empty();

            long sz = Files.size(data);
            if (sz < 0 || sz > maxBytesPerEntry) {
                safeDelete(dir);
                return Optional.empty();
            }

            var m = om.readValue(Files.readAllBytes(meta), Meta.class);
            byte[] bytes = Files.readAllBytes(data);

            Instant lm = (m.lastModifiedEpochMs != null && m.lastModifiedEpochMs > 0)
                    ? Instant.ofEpochMilli(m.lastModifiedEpochMs) : null;

            return Optional.of(new ConfigEnvelope(bytes, m.etag, m.contentType, lm));
        } catch (IOException ex) {
            safeDelete(dir);
            return Optional.empty();
        }
    }

    @Override
    public void put(String path, ConfigEnvelope env) {
        if (!enabled || path == null || path.isBlank() || env == null || env.bytes() == null) return;

        byte[] bytes = env.bytes();
        if (bytes.length > maxBytesPerEntry) {
            evict(path);
            return;
        }

        var dir = entryDir(path);
        var meta = dir.resolve("meta.json");
        var data = dir.resolve("data.bin");

        try {
            Files.createDirectories(dir);

            var tmpData = dir.resolve("data.bin.tmp");
            Files.write(tmpData, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmpData, data, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpData, data, StandardCopyOption.REPLACE_EXISTING);
            }

            var m = new Meta();
            m.etag = env.etag();
            m.contentType = env.contentType();
            m.lastModifiedEpochMs = env.lastModified() != null ? env.lastModified().toEpochMilli() : null;
            m.hash = sha256Hex(bytes);

            var tmpMeta = dir.resolve("meta.json.tmp");
            Files.writeString(tmpMeta, om.writeValueAsString(m),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmpMeta, meta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpMeta, meta, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            safeDelete(dir);
        }
    }

    @Override
    public void evict(String path) {
        if (!enabled || path == null || path.isBlank()) return;
        var dir = entryDir(path);
        safeDelete(dir);
    }

    // ===== Helpers

    private static Path toRoot(ConfigProps.L2 l2) {
        String dir = (l2 != null && l2.dir() != null && !l2.dir().isBlank())
                ? l2.dir() : "/tmp/config-l2-cache";
        return Paths.get(dir).toAbsolutePath().normalize();
    }

    private Path entryDir(String path) {
        String h = sha256Hex(path);
        String p1 = h.substring(0, 2);
        String p2 = h.substring(2, 4);
        return root.resolve(p1).resolve(p2).resolve(h);
    }

    private static void safeDelete(Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                try (var s = Files.walk(dir)) {
                    s.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
                }
            }
        } catch (IOException ignored) {}
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        }
    }

    // metadata lưu kèm mỗi entry
    private static final class Meta {
        public String etag;
        public String contentType;
        public Long   lastModifiedEpochMs;
        public String hash;
    }
}