package com.SouthMillion.guild_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Guild Service Application
 * 
 * P3 Social Service - Guild Management System
 * 
 * Features:
 * - Guild creation and management
 * - Member management (50 max members)
 * - Guild technology upgrades (5 branches)
 * - Guild warehouse (100 slots)
 * - Application system
 * - Leadership transfer
 * - Donation and contribution tracking
 * 
 * @author Game Server Team
 * @version 1.0.0
 * @since 2026-01-30
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class GuildServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuildServiceApplication.class, args);
    }
}
