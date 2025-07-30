package com.southMillion.webSocket_server.service.client.user;

import org.SouthMillion.dto.user.RoleInfoDTO;
import org.SouthMillion.dto.user.UserInfoDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserGameFeignClient {

    @GetMapping("/api/usergame/noticetime/{uid}")
    long getNoticeTime(@PathVariable("uid") Integer uid);

    @PostMapping("/api/usergame/noticetime/{uid}")
    void setNoticeTime(@PathVariable("uid") Integer uid, @RequestParam("time") long time);

    @PostMapping("/api/usergame/cmd2client/{uid}")
    void sendCmdToClient(@PathVariable("uid") Integer uid, @RequestParam("cmdId") int cmdId, @RequestBody byte[] str);

    @PostMapping("/api/usergame/limitcore/{uid}")
    void limitCoreOp(@PathVariable("uid") Integer uid, @RequestParam("type") int type, @RequestParam("p1") int p1);


    @GetMapping("/api/usergame/auth/login")
    LoginResponseDTO login(
            @RequestParam("spid") String spid,
            @RequestParam("device") String device,
            @RequestParam("userId") String userId,
            @RequestParam("timestamp") Long timestamp,
            @RequestParam("sign") String sign
    );

    @GetMapping("/api/usergame/info")
    UserInfoDTO getAccountInfo(@RequestParam("platUserName") String platUserName);

    @GetMapping("/api/usergame/role-info")
    RoleInfoDTO getRoleInfo(@RequestParam("roleId") Integer roleId);

    @GetMapping("/api/usergame/system-setting")
    List<Msgrole.PB_system_set> getSystemSetting(@RequestParam("roleId") Integer roleId);

    @PostMapping("/api/usergame/system-setting")
    void saveSystemSetting(@RequestParam("roleId") Integer roleId, @RequestBody List<Msgrole.PB_system_set> settingList);

    @PostMapping("/api/usergame/gm-command")
    void gmCommand(@RequestParam("roleId") Integer roleId, @RequestParam("type") String type,
                   @RequestParam("command") String command, @RequestParam("result") String result);

    @GetMapping("/api/usergame/roles")
    List<Integer> getRoleIds(@RequestParam("platUserName") String platUserName);
}
