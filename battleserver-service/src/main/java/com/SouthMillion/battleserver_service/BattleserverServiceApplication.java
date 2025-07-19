package com.SouthMillion.battleserver_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BattleserverServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BattleserverServiceApplication.class, args);
	}

}
