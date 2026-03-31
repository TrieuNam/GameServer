package com.SouthMillion.pagoda_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.SouthMillion")
public class PagodaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PagodaServiceApplication.class, args);
    }
}
