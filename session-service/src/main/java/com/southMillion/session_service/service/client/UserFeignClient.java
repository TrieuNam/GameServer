package com.southMillion.session_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Giả định user-service có endpoint nội bộ để verify:
 * POST /internal/user/verify { username, password } -> { ok: true, userId: "...", username: "..." }
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @PostMapping("/internal/user/verify")
    Map<String,Object> verify(@RequestBody Map<String,String> body);
}