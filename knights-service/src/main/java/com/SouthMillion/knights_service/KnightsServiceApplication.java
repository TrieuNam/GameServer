package com.SouthMillion.knights_service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
public class KnightsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnightsServiceApplication.class, args);
    }
}
