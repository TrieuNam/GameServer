package com.SouthMillion.gameworld_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableFeignClients
@EnableCaching
public class GameworldServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameworldServiceApplication.class, args);
	}

}
