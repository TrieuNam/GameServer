package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.UserGameService;
import org.SouthMillion.dto.user.*;
import org.SouthMillion.dto.user.request.LoginRequestDTO;
import org.SouthMillion.dto.user.request.RegisterRequestDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.dto.user.response.RegisterResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usergame")
public class UserGameController {
    @Autowired
    private UserGameService service;

    @PostMapping("/auth/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO req) {
        return service.login(req);
    }
    @GetMapping("/roleinfo/{uid}")
    public RoleDTO getRoleInfo(@PathVariable Integer uid) {
        return service.getRoleInfo(uid);
    }
    @GetMapping("/roleattr/{uid}")
    public RoleAttrListDTO getRoleAttrList(@PathVariable Integer uid) {
        return service.getRoleAttrList(uid);
    }
    @PostMapping("/changeexp")
    public RoleExpChangeDTO changeExp(@RequestParam Integer uid, @RequestParam long exp) {
        return service.changeExp(uid, exp);
    }
    @PostMapping("/levelup/{uid}")
    public RoleLevelChangeDTO levelUp(@PathVariable Integer uid) {
        return service.levelUp(uid);
    }
    @PostMapping("/systemset/{uid}")
    public void setSystemSettings(@PathVariable Integer uid, @RequestBody List<SystemSetDTO> list) {
        service.setSystemSettings(uid, list);
    }
    @GetMapping("/systemset/{uid}")
    public RoleSystemSetDTO getSystemSettings(@PathVariable Integer uid) {
        return service.getSystemSettings(uid);
    }
    @GetMapping("/noticetime/{uid}")
    public long getNoticeTime(@PathVariable Integer uid) {
        return service.getNoticeTime(uid);
    }
    @PostMapping("/noticetime/{uid}")
    public void setNoticeTime(@PathVariable Integer uid, @RequestParam long time) {
        service.setNoticeTime(uid, time);
    }
    @PostMapping("/cmd2client/{uid}")
    public void sendCmdToClient(@PathVariable Integer uid, @RequestParam int cmdId, @RequestBody byte[] str) {
        service.sendCmdToClient(uid, cmdId, str);
    }
    @PostMapping("/limitcore/{uid}")
    public void limitCoreOp(@PathVariable Integer uid, @RequestParam int type, @RequestParam int p1) {
        service.limitCoreOp(uid, type, p1);
    }
    @GetMapping("/limitcore/{uid}")
    public LimitCoreInfoDTO getLimitCoreInfo(@PathVariable Integer uid) {
        return service.getLimitCoreInfo(uid);
    }
    @PostMapping("/gm/{uid}")
    public void sendGMCommand(@PathVariable Integer uid, @RequestParam byte[] type, @RequestParam byte[] cmd) {
        service.sendGMCommand(uid, type, cmd);
    }
    @GetMapping("/gm/{uid}")
    public GMCommandResultDTO getGMCommandResult(@PathVariable Integer uid) {
        return service.getGMCommandResult(uid);
    }
    @GetMapping("/otherrole/{otherUid}")
    public RoleDTO getOtherRoleInfo(@PathVariable Integer otherUid) {
        return service.getOtherRoleInfo(otherUid);
    }

}