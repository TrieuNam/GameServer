package com.SouthMillion.lingzhu_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
public class LingZhuServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LingZhuServiceApplication.class, args);
    }
}
