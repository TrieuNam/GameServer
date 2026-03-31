package com.SouthMillion.mount_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mount Service Application
 * Handles mount/cavalry system including harness equipment, mount upgrades, and exploration
 * 
 * MsgIDs: 2140-2149
 * Operations: LEVEL_UP, GRADE_UP, EXPLORE, SET_APP, PIFU_UP, SET_PIFU, WEAR, DECOMPOSE, UNLOCK, etc.
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
public class MountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MountServiceApplication.class, args);
    }
}
