package com.SouthMillion.common.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis optimization for low memory
 * Reduces IO threads and computation threads
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisOptimizationConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisOptimizationConfig.class);

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        // Detect service tier
        String appName = System.getProperty("spring.application.name", "");
        ServiceTier tier = detectTier(appName);
        
        // Reduce thread pools based on tier
        int ioThreads = tier == ServiceTier.ULTRA_LOW ? 2 : (tier == ServiceTier.CRITICAL ? 4 : 2);
        int computeThreads = tier == ServiceTier.ULTRA_LOW ? 2 : (tier == ServiceTier.CRITICAL ? 4 : 2);
        
        ClientResources resources = DefaultClientResources.builder()
                .ioThreadPoolSize(ioThreads)
                .computationThreadPoolSize(computeThreads)
                .build();
        
        log.info("🔴 Redis ClientResources optimized - Tier: {}, IO: {}, Compute: {}", 
                 tier, ioThreads, computeThreads);
        
        return resources;
    }
    
    private ServiceTier detectTier(String appName) {
        if (appName.contains("eureka") || appName.contains("gateway") || appName.contains("config")) {
            return ServiceTier.CRITICAL;
        }
        if (appName.contains("analytics") || appName.contains("scheduler") || 
            appName.contains("file") || appName.contains("localization")) {
            return ServiceTier.ULTRA_LOW;
        }
        return ServiceTier.MINIMAL;
    }
    
    private enum ServiceTier {
        CRITICAL, MINIMAL, ULTRA_LOW
    }
}
