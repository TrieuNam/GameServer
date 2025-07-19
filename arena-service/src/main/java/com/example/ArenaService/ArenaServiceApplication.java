package com.example.ArenaService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ArenaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArenaServiceApplication.class, args);
	}

}
