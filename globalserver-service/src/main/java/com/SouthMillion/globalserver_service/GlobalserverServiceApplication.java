package com.SouthMillion.globalserver_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GlobalserverServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GlobalserverServiceApplication.class, args);
	}

}
