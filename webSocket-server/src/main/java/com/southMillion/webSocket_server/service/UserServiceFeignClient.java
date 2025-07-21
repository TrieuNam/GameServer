package com.southMillion.webSocket_server.service;

import org.SouthMillion.dto.user.LoginVerify;
import org.SouthMillion.dto.user.RoleAttrDto;
import org.SouthMillion.dto.user.RoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
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

    @GetMapping("/user/roleInfo")
    RoleDto getRoleInfo(@RequestParam("roleId") String roleId);

    @GetMapping("/user/roleAttrs")
    List<RoleAttrDto> getRoleAttrs(@RequestParam("roleId") String roleId);

    @GetMapping("/user/roleExp")
    Long getRoleExp(@RequestParam("roleId") String roleId);

    @GetMapping("/user/roleLevel")
    Integer getRoleLevel(@RequestParam("roleId") String roleId);
}