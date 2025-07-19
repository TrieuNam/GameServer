package com.SouthMillion.task_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface AuthVerifyFeignClient {
    @PostMapping("/api/auth/verify")
    Map<String, Object> verifyToken(@RequestBody Map<String, String> body);
}