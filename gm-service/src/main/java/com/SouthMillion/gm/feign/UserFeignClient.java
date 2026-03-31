package com.SouthMillion.gm.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserFeignClient {
    
    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUserInfo(@PathVariable Long userId);
    
    @PostMapping("/api/users/{userId}/ban")
    Map<String, Object> banUser(@PathVariable Long userId, @RequestBody Map<String, Object> request);
    
    @PostMapping("/api/users/{userId}/unban")
    Map<String, Object> unbanUser(@PathVariable Long userId);
}
