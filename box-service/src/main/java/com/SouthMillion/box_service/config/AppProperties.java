package com.SouthMillion.box_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app")
public class AppProperties {
    private Config config = new Config();
    private Cache cache = new Cache();
    private Rng rng = new Rng();

    @Data public static class Config {
        private String serviceName;
        private String unpackPath;
        private String kaixiangPath;
    }
    @Data public static class Cache { private boolean l1Enabled = true; }
    @Data public static class Rng { private boolean seeded = false; }
}