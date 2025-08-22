package com.SouthMillion.config_service.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.SouthMillion.dto.config.ConfigFileData;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@TestConfiguration
public class TestWebConfig {

    @Bean
    public Cache<String, ConfigFileData> testL1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(Duration.ofMinutes(5))
                .build();
    }
}