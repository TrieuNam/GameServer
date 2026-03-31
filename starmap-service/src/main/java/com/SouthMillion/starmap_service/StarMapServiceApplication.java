package com.SouthMillion.starmap_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Star Map Service Application
 * Handles star map/constellation system for celestial power
 * 
 * MsgIDs: 2150-2159
 * C++ Source: gameworld/other/starmap/
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
public class StarMapServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StarMapServiceApplication.class, args);
    }
}
