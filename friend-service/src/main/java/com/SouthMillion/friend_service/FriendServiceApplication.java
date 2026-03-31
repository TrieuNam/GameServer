package com.SouthMillion.friend_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Friend Service Application
 * 
 * P3 Social Service - Friend Management System
 * 
 * Features:
 * - Friend list management (max 100 friends)
 * - Friend requests (send/approve/reject)
 * - Search players
 * - Block/unblock players
 * - Online status tracking
 * - Gift giving system
 * - Friend chat
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
public class FriendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriendServiceApplication.class, args);
    }
}
