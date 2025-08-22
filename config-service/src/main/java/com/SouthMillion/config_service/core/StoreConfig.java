package com.SouthMillion.config_service.core;

import com.SouthMillion.config_service.config.ConfigProperties;
import com.SouthMillion.config_service.config.FileSystemConfigStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.SouthMillion.dto.config.ConfigFileData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class StoreConfig {

    @Bean
    public ConfigStore configStore(ConfigProperties props) {
        if (props.getMode() == ConfigProperties.Mode.FILESYSTEM) {
            Path root = Path.of(props.getRoot(), "config").toAbsolutePath().normalize();
            return new FileSystemConfigStore(root);
        }
        return new ClasspathConfigStore();
    }

    // ======== BẢN ĐƠN GIẢN (an toàn, không parse spec) ========
    // Bỏ comment nếu bạn không muốn parse spec từ YAML.
    /*
    @Bean
    public Cache<String, ConfigFileData> l1Cache(ConfigProperties props) {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }
    */

    // ======== BẢN CÓ PARSER CHO l1-spec ========
    @Bean
    public Cache<String, ConfigFileData> l1Cache(ConfigProperties props) {
        var spec = props.getCache().getL1Spec(); // ví dụ: "maximumSize=5000,expireAfterAccess=10m"
        long maximumSize = parseMaximumSize(spec, 5000);
        Duration expireAfterAccess = parseExpireAfterAccess(spec, Duration.ofMinutes(10));
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(expireAfterAccess)
                .build();
    }

    private static long parseMaximumSize(String spec, long defVal) {
        String v = extract(spec, "maximumSize");
        if (v == null) return defVal;
        try { return Long.parseLong(v); } catch (NumberFormatException ignored) { return defVal; }
    }

    private static Duration parseExpireAfterAccess(String spec, Duration defVal) {
        String v = extract(spec, "expireAfterAccess");
        if (v == null) return defVal;
        return parseDuration(v, defVal);
    }

    private static String extract(String spec, String key) {
        if (spec == null) return null;
        // tách theo dấu phẩy, cho phép khoảng trắng
        String[] parts = spec.split("\\s*,\\s*");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i <= 0) continue;
            String k = p.substring(0, i).trim();
            String val = p.substring(i + 1).trim();
            if (k.equals(key)) return val;
        }
        return null;
    }

    // Hỗ trợ hậu tố: s, m, h, d (giây, phút, giờ, ngày)
    private static final Pattern DUR = Pattern.compile("^([0-9]+)\\s*([smhdSMHD])?$");

    private static Duration parseDuration(String txt, Duration defVal) {
        Matcher m = DUR.matcher(txt);
        if (!m.matches()) return defVal;
        long num = Long.parseLong(m.group(1));
        String unit = m.group(2);
        if (unit == null) return Duration.ofSeconds(num); // mặc định giây nếu không có hậu tố
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "s": return Duration.ofSeconds(num);
            case "m": return Duration.ofMinutes(num);
            case "h": return Duration.ofHours(num);
            case "d": return Duration.ofDays(num);
            default:  return defVal;
        }
    }
}