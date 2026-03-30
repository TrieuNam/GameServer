package com.SouthMillion.rune_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Rune Service Application
 * Handles rune enhancement system for equipment power boost
 * 
 * MsgIDs: 1670-1672
 * C++ Source: gameworld/other/rune/
 * Config: rune.json
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
public class RuneServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuneServiceApplication.class, args);
    }
}
