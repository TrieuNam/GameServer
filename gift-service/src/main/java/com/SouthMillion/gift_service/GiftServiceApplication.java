package com.SouthMillion.gift_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableFeignClients
public class GiftServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GiftServiceApplication.class, args);
    }

}

