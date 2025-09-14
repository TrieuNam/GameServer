package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.SouthMillion.dto.role.mail.MailDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "role-service")
public interface RoleFeign {
    // Role
    @GetMapping("/api/role/by-user/{userId}")
    List<RoleDTOs.RoleResp> listByUser(@PathVariable("userId") String userId);

    @PostMapping("/api/role")
    RoleDTOs.RoleResp create(@RequestBody RoleDTOs.CreateRoleReq req);

    @PostMapping("/api/role/{roleId}/wxinfo")
    RoleDTOs.RoleResp setWxInfo(@PathVariable("roleId") String roleId,
                                @RequestBody RoleDTOs.WxInfoSetReq body);

    // Settings
    @PostMapping("/api/role/settings")
    SettingsDTOs.SystemSetResp applySettings(@RequestBody SettingsDTOs.SystemSetReq req);

    // Mail
    @PostMapping("/api/mail/list")
    MailDTOs.MailListResp mailList(@RequestBody MailDTOs.MailListReq req);

    @GetMapping("/api/mail/{userId}/{mailId}")
    MailDTOs.MailDetailResp mailDetail(@PathVariable String userId, @PathVariable String mailId);

    @PostMapping("/api/mail/{userId}/{mailId}/delete")
    MailDTOs.MailDeleteResp mailDelete(@PathVariable String userId, @PathVariable String mailId);

    @PostMapping("/api/mail/{userId}/{mailId}/fetch")
    MailDTOs.FetchMailResp mailFetch(@PathVariable String userId, @PathVariable String mailId);

    // Ads
    @PostMapping("/api/ads/claim")
    AdvertisementDTOs.AdInfo claimAd(@RequestBody AdvertisementDTOs.AdFetchReq req);

    // Other Role
    @GetMapping("/api/other-role/{uid}")
    OtherRoleDTOs.OtherRoleInfo getOtherRole(@PathVariable String uid,
                                             @RequestParam(value="roleId", required=false) String roleId);
}