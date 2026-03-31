package com.SouthMillion.moderation_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableKafka
@EnableAsync
@EnableScheduling
public class ModerationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }
}
