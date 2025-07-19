package com.southMillion.webSocket_server.service;

import org.SouthMillion.dto.user.LoginVerify;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

// Tên "user-service" phải đúng tên bạn đăng ký trên Eureka/Nacos
@FeignClient(name = "user-service")
public interface UserServiceFeignClient {
    @GetMapping("/auth/login")
    LoginVerify login(
            @RequestParam("spid") String spid,
            @RequestParam("device") String device,
            @RequestParam("userId") String userId,
            @RequestParam("timestamp") Long timestamp,
            @RequestParam(value = "sign", required = false) String sign
    );
}