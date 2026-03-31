package com.SouthMillion.territory_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Territory Service Application
 * Handles territory/base management system for land control and resource production
 *
 * Features:
 * - Territory ownership and levels
 * - Building construction and upgrades
 * - Resource production (auto-generated over time)
 * - Defense and attack ratings
 * - Tax collection from production
 */
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class TerritoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TerritoryServiceApplication.class, args);
    }
}
