package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.user.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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


    @PostMapping("/user/settings")
    void updateSettings(@RequestBody UserSettingsDto dto);

    @GetMapping("/user/settings")
    UserSettingsDto getSettings(@RequestParam("userKey") String userKey);

    @PostMapping("/gm/ban")
    void banUser(@RequestParam String userId);

    @PostMapping("/gm/unban")
    void unbanUser(@RequestParam String userId);

    @PostMapping("/gm/reset")
    void resetUser(@RequestParam String userId);


    @PostMapping("/api/limit-core/get")
    LimitCoreInfoDto getCoreInfo(@RequestBody LimitCoreReqDto req);

    @PostMapping("/api/limit-core/update")
    LimitCoreInfoDto updateCoreInfo(@RequestBody LimitCoreReqDto req);


    @PostMapping("/api/notice-time/get")
    NoticeTimeRespDto getNoticeTime(@RequestBody NoticeTimeReqDto req);

    @PostMapping("/api/notice-time/set")
    NoticeTimeRespDto setNoticeTime(@RequestBody NoticeTimeReqDto req);
}