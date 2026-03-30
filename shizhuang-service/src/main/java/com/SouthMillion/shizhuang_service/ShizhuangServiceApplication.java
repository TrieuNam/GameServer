package com.SouthMillion.shizhuang_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EntityScan("com.SouthMillion")
@EnableJpaRepositories("com.SouthMillion")
@EnableFeignClients(basePackages = "com.SouthMillion")
public class ShizhuangServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShizhuangServiceApplication.class, args);
    }
}
