package com.SouthMillion.drop_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
        String dropPathTemplate = "gameworld/drop/%s.xml";
        List<Integer> knownDropIds = new ArrayList<>();
        List<String> knownDropRanges = new ArrayList<>();

        public List<Integer> resolveKnownDropIds() {
            Set<Integer> merged = new TreeSet<>();
            if (knownDropIds != null) {
                merged.addAll(knownDropIds);
            }
            if (knownDropRanges != null) {
                for (String raw : knownDropRanges) {
                    if (!StringUtils.hasText(raw)) {
                        continue;
                    }
                    String token = raw.trim();
                    if (token.contains("-")) {
                        String[] parts = token.split("-", 2);
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        if (end < start) {
                            int tmp = start;
                            start = end;
                            end = tmp;
                        }
                        for (int id = start; id <= end; id++) {
                            merged.add(id);
                        }
                    } else {
                        merged.add(Integer.parseInt(token));
                    }
                }
            }
            return new ArrayList<>(merged);
        }
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