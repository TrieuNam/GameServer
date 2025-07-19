package com.SouthMillion.world_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WorldServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldServiceApplication.class, args);
    }

}
