package com.SouthMillion.leaderboard_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Leaderboard Service Application
 *
 * Features:
 * - Power ranking (top 100)
 * - Level ranking
 * - Arena ranking
 * - Wealth ranking (gold + gems)
 * - Guild ranking
 * - Pet ranking
 * - Mount ranking
 * - Weekly/Monthly/Season rankings
 * - Redis caching for performance
 * - Auto-refresh every 5 minutes
 *
 * @author LHP Game Server
 * @version 1.0.0
 */
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class LeaderboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderboardServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Leaderboard Service Started!");
        System.out.println("Port: 8480");
        System.out.println("Database: leaderboard_db");
        System.out.println("========================================");
    }
}
