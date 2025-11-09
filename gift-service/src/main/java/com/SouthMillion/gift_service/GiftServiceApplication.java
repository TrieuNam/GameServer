package com.SouthMillion.gift_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GiftServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GiftServiceApplication.class, args);
    }

}

