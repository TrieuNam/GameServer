package com.SouthMillion.webSocket_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.SouthMillion")
@EnableDiscoveryClient
@EnableFeignClients
public class WebSocketServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebSocketServerApplication.class, args);
	}

}
