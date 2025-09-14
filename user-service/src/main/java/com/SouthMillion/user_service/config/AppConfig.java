package com.SouthMillion.user_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@Configuration
@EnableCaching
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt mặc định (delegating encoder)
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)); // TTL mặc định

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                // tuỳ biến TTL theo cache name
                .withCacheConfiguration("userActive", defaults.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}