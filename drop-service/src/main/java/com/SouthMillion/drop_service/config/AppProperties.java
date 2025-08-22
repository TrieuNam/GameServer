package com.SouthMillion.drop_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    Config config = new Config();
    Cache cache = new Cache();
    Item item = new Item();
    Pity pity = new Pity();
    Bag bag = new Bag();

    @Data public static class Config {
        String serviceName = "config-service";
        int managerReloadSeconds = 60;
    }
    @Data public static class Cache {
        int compiledTtlMinutes = 10;
        int compiledMaxSize = 500;
    }
    @Data public static class Item { boolean validate = false; }
    @Data public static class Bag {
        boolean applyEnabled = false;
        int defaultBagType = 0;
    }
    @Data public static class Pity {
        boolean enabled = false;
        int defaultThreshold = 50;
        String defaultRareSelector = "BROADCAST";
        Map<String, PerDrop> perDrop = new HashMap<>();
        @Data public static class PerDrop {
            Integer threshold;
            String rareSelector; // BROADCAST | LIST
            List<Integer> rareItemIds;
        }
    }
}