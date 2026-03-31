package com.SouthMillion.angel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Angel Service Application
 * Handles angel/wing companion system with upgrades, skills, and appearance customization
 * 
 * MsgIDs: 2130-2132
 * C++ Source: gameworld/other/angel/
 * Config: angel.json
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
public class AngelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AngelServiceApplication.class, args);
    }
}
