package com.SouthMillion.config_service.core;

import com.SouthMillion.config_service.config.ConfigProps;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.apache.tika.Tika;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;


@Component
public class ClasspathConfigRepository implements ConfigRepository {

    private final String root;  // ví dụ: "config"
    private final Tika tika = new Tika();

    public ClasspathConfigRepository(ConfigProps props) {
        String cp = props.classpathRoot();
        this.root = (cp == null || cp.isBlank()) ? "config" : trimSlashes(cp);
    }

    @Override
    public Optional<ConfigEnvelope> read(String path) {
        String norm = normalizePath(path);
        if (norm.isEmpty()) return Optional.empty();

        String location = root + "/" + norm; // ví dụ: config/logicconfig/roleexp.json
        ClassPathResource res = new ClassPathResource(location);

        if (!res.exists() || !res.isReadable()) return Optional.empty();

        try (InputStream in = res.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String etag = sha256Hex(bytes);
            String filename = res.getFilename();
            String contentType = safeDetect(bytes, filename);

            Instant lastModified = null;
            try {
                long lm = res.lastModified();
                if (lm > 0) lastModified = Instant.ofEpochMilli(lm);
            } catch (IOException ignored) {
                // trong JAR có thể không lấy được lastModified
            }

            return Optional.of(new ConfigEnvelope(bytes, etag, contentType, lastModified));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String path) {
        String norm = normalizePath(path);
        if (norm.isEmpty()) return false;
        return new ClassPathResource(root + "/" + norm).exists();
    }

    /** Enumerate classpath là best-effort → trả rỗng để an toàn. */
    @Override
    public List<String> list(String prefix) {
        return Collections.emptyList();
    }

    // ===== Helpers

    private static String normalizePath(String p) {
        if (p == null) return "";
        String s = p.replace('\\', '/').trim();
        if (s.startsWith("/")) s = s.substring(1);
        if (s.contains("..")) return "";
        while (s.contains("//")) s = s.replace("//", "/");
        return s;
    }

    private static String trimSlashes(String p) {
        if (p == null) return "";
        String s = p.replace('\\', '/');
        while (s.startsWith("/")) s = s.substring(1);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
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

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(data));
        }
    }
}