package com.SouthMillion.chat_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Chat Service Application
 * 
 * P3 Social Service - Chat System
 * 
 * Features:
 * - World chat (visible to all)
 * - Guild chat (guild members only)
 * - Team chat (team members only)
 * - Private chat (1-on-1)
 * - System announcements
 * - Chat history
 * - Mute/unmute players
 * - Chat reports
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
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
