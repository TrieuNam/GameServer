package com.SouthMillion.main_fb_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class MainFbServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainFbServiceApplication.class, args);
    }
}
