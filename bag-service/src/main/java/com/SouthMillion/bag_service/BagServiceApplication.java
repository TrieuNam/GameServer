package com.SouthMillion.bag_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class BagServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BagServiceApplication.class, args);
	}

}
