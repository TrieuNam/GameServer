package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.UserService;
import org.SouthMillion.dto.user.RoleInfoDTO;
import org.SouthMillion.dto.user.UserInfoDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usergame")
public class UserController {

    @Autowired
    private UserService userService;

    // 1. Login check (dùng cho service nội bộ hoặc test)

    @GetMapping("/auth/login")
    public LoginResponseDTO login(
            @RequestParam String spid,
            @RequestParam String device,
            @RequestParam String userId,
            @RequestParam Long timestamp,
            @RequestParam String sign) {
        // Lưu ý: userId ở đây mapping với platUserName ở bảng login!
        return userService.loginCheck(spid, device, userId, timestamp, sign);
    }

    @GetMapping("/roles")
    public List<Integer> getRoleIds(@RequestParam String platUserName) {
        // Trả về danh sách roleId theo tài khoản
        return userService.getRoleIds(platUserName);
    }

    // 2. Account info
    @GetMapping("/info")
    public UserInfoDTO getAccountInfo(@RequestParam String platUserName) {
        return userService.getAccountInfo(platUserName);
    }

    // 3. Role info
    @GetMapping("/role-info")
    public RoleInfoDTO getRoleInfo(@RequestParam Integer roleId) {
        return userService.getRoleInfo(roleId);
    }

    // 4. Get system setting
    @GetMapping("/system-setting")
    public List<Msgrole.PB_system_set> getSystemSetting(@RequestParam Integer roleId) {
        return userService.getSystemSetting(roleId);
    }

    // 5. Save system setting
    @PostMapping("/system-setting")
    public void saveSystemSetting(@RequestParam Integer roleId, @RequestBody List<Msgrole.PB_system_set> settingList) {
        userService.saveSystemSetting(roleId, settingList);
    }

    // 6. GM Command (optional, dùng cho backend quản trị)
    @PostMapping("/gm-command")
    public void gmCommand(@RequestParam Integer roleId, @RequestParam String type,
                          @RequestParam String command, @RequestParam String result) {
        userService.saveGMCommand(roleId, type, command, result);
    }

    @GetMapping("/api/user/level")
    public Integer getUserLevel(@RequestParam("userId") String userId) {
        return userService.getUserLevel(userId);
    }
}