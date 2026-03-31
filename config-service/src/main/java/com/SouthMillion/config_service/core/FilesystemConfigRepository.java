package com.SouthMillion.config_service.core;

import com.SouthMillion.config_service.config.ConfigProps;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class FilesystemConfigRepository implements ConfigRepository {

    private final Path root;   // ví dụ: /opt/app/config
    private final Tika tika = new Tika();

    public FilesystemConfigRepository(ConfigProps props) {
        String fs = props.fsRoot();
        if (fs == null || fs.isBlank()) {
            this.root = Paths.get("config").toAbsolutePath().normalize();
        } else {
            this.root = Paths.get(fs).toAbsolutePath().normalize();
        }
    }

    @Override
    public Optional<ConfigEnvelope> read(String path) {
        Path p = resolveSafe(path);
        if (p == null) return Optional.empty();
        if (!Files.isRegularFile(p)) return Optional.empty();

        try {
            byte[] bytes = Files.readAllBytes(p);
            String etag = sha256Hex(bytes);
            String contentType = safeDetect(bytes, p.getFileName().toString());
            Instant lastModified = Files.getLastModifiedTime(p).toInstant();

            return Optional.of(new ConfigEnvelope(bytes, etag, contentType, lastModified));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String path) {
        Path p = resolveSafe(path);
        return p != null && Files.isRegularFile(p);
    }

    @Override
    public List<String> list(String prefix) {
        String normPrefix = normalizePrefix(prefix);
        List<String> out = new ArrayList<>();
        try {
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile).forEach(f -> {
                    String rel = root.relativize(f).toString().replace('\\', '/');
                    if (rel.startsWith(normPrefix)) out.add(rel);
                });
            }
        } catch (IOException ignored) {}
        return out;
    }

    // ===== Helpers

    private Path resolveSafe(String input) {
        if (input == null) return null;
        String s = input.replace('\\', '/').trim();
        if (s.startsWith("/")) s = s.substring(1);
        if (s.contains("..")) return null;
        while (s.contains("//")) s = s.replace("//", "/");

        Path p = root.resolve(s).normalize();
        if (!p.startsWith(root)) return null; // chống thoát root
        return p;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) return "";
        String s = prefix.replace('\\', '/').trim();
        if (s.startsWith("/")) s = s.substring(1);
        if (s.contains("..")) return "";
        while (s.contains("//")) s = s.replace("//", "/");
        return s;
    }

    private String safeDetect(byte[] bytes, String filename) {
        try {
            if (filename != null) return tika.detect(bytes, filename);
            return tika.detect(bytes);
        } catch (Throwable ignore) {
            String txt = new String(bytes, StandardCharsets.UTF_8).trim();
            if (txt.startsWith("{") || txt.startsWith("[")) return "application/json";
            return "text/plain";
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        }
    }
}