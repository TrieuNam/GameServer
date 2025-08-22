package com.SouthMillion.config_service.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties under prefix: "config"
 *
 * YAML mẫu:
 * config:
 *   mode: classpath   # classpath | filesystem
 *   root: ""          # nếu mode=filesystem thì bắt buộc chỉ định path
 *   cache:
 *     l1-enabled: true
 *     l1-spec: "maximumSize=5000,expireAfterAccess=10m"
 */
@Validated
@ConfigurationProperties(prefix = "config")
public class ConfigProperties {

    /** Nguồn đọc cấu hình: CLASSPATH hoặc FILESYSTEM. Mặc định CLASSPATH. */
    @NotNull
    private Mode mode = Mode.CLASSPATH;

    /** Thư mục gốc khi mode=FILESYSTEM (ví dụ: D:/configs hoặc /opt/app/configs). */
    private String root = "";

    /** Cấu hình cache L1 trong process. */
    @NotNull
    private Cache cache = new Cache();

    public enum Mode {
        CLASSPATH, FILESYSTEM
    }

    // ===== Validation nâng cao =====
    @AssertTrue(message = "config.root must be non-empty when config.mode=FILESYSTEM")
    public boolean isRootValid() {
        return mode != Mode.FILESYSTEM || (root != null && !root.isBlank());
    }

    // ===== Getters/Setters =====
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    // ===== Nested "cache" group =====
    public static class Cache {
        /** Bật/tắt L1 cache (Caffeine) trong service. */
        private boolean l1Enabled = true;

        /** Caffeine spec string, ví dụ: "maximumSize=5000,expireAfterAccess=10m". */
        private String l1Spec = "maximumSize=5000,expireAfterAccess=10m";

        public boolean isL1Enabled() {
            return l1Enabled;
        }

        public void setL1Enabled(boolean l1Enabled) {
            this.l1Enabled = l1Enabled;
        }

        public String getL1Spec() {
            return l1Spec;
        }

        public void setL1Spec(String l1Spec) {
            this.l1Spec = l1Spec;
        }
    }
}