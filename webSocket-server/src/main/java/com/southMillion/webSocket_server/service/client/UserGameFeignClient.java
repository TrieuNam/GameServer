package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.user.*;
import org.SouthMillion.dto.user.request.LoginRequestDTO;
import org.SouthMillion.dto.user.request.RegisterRequestDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.dto.user.response.RegisterResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserGameFeignClient {
    @PostMapping("/api/usergame/login")
    LoginResponseDTO login(@RequestBody LoginRequestDTO req);

    @GetMapping("/api/usergame/roleinfo/{uid}")
    RoleDTO getRoleInfo(@PathVariable("uid") Integer uid);

    @GetMapping("/api/usergame/roleattr/{uid}")
    RoleAttrListDTO getRoleAttrList(@PathVariable("uid") Integer uid);

    @PostMapping("/api/usergame/changeexp")
    RoleExpChangeDTO changeExp(@RequestParam("uid") Integer uid, @RequestParam("exp") long exp);

    @PostMapping("/api/usergame/levelup/{uid}")
    RoleLevelChangeDTO levelUp(@PathVariable("uid") Integer uid);

    @PostMapping("/api/usergame/systemset/{uid}")
    void setSystemSettings(@PathVariable("uid") Integer uid, @RequestBody List<SystemSetDTO> list);

    @GetMapping("/api/usergame/systemset/{uid}")
    RoleSystemSetDTO getSystemSettings(@PathVariable("uid") Integer uid);

    @GetMapping("/api/usergame/noticetime/{uid}")
    long getNoticeTime(@PathVariable("uid") Integer uid);

    @PostMapping("/api/usergame/noticetime/{uid}")
    void setNoticeTime(@PathVariable("uid") Integer uid, @RequestParam("time") long time);

    @PostMapping("/api/usergame/cmd2client/{uid}")
    void sendCmdToClient(@PathVariable("uid") Integer uid, @RequestParam("cmdId") int cmdId, @RequestBody byte[] str);

    @PostMapping("/api/usergame/limitcore/{uid}")
    void limitCoreOp(@PathVariable("uid") Integer uid, @RequestParam("type") int type, @RequestParam("p1") int p1);

    @GetMapping("/api/usergame/limitcore/{uid}")
    LimitCoreInfoDTO getLimitCoreInfo(@PathVariable("uid") Integer uid);

    @PostMapping("/api/usergame/gm/{uid}")
    void sendGMCommand(@PathVariable("uid") Integer uid, @RequestParam("type") byte[] type, @RequestParam("cmd") byte[] cmd);

    @GetMapping("/api/usergame/gm/{uid}")
    GMCommandResultDTO getGMCommandResult(@PathVariable("uid") Integer uid);

    @GetMapping("/api/usergame/otherrole/{otherUid}")
    RoleDTO getOtherRoleInfo(@PathVariable("otherUid") Integer otherUid);

    @PostMapping("/api/usergame/register")
    RegisterResponseDTO register(@RequestBody RegisterRequestDTO req);
}
