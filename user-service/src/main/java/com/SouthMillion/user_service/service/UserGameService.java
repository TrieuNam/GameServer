package com.SouthMillion.user_service.service;

import org.SouthMillion.dto.user.*;
import org.SouthMillion.dto.user.request.LoginRequestDTO;
import org.SouthMillion.dto.user.request.RegisterRequestDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.dto.user.response.RegisterResponseDTO;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface UserGameService {
    LoginResponseDTO login(LoginRequestDTO req); // 7000/7056

    RoleDTO getRoleInfo(Integer uid);           // 1400

    RoleAttrListDTO getRoleAttrList(Integer uid); // 1401

    RoleExpChangeDTO changeExp(Integer uid, long expChange); // 1402

    RoleLevelChangeDTO levelUp(Integer uid);    // 1403

    void setSystemSettings(Integer uid, List<SystemSetDTO> systemSetList); // 1460

    RoleSystemSetDTO getSystemSettings(Integer uid); // 1461

    long getNoticeTime(Integer uid);            // 1464

    void setNoticeTime(Integer uid, long time); // 1465

    void sendCmdToClient(Integer uid, int cmdId, byte[] str); // 1466

    void limitCoreOp(Integer uid, int type, int p1); // 1467

    LimitCoreInfoDTO getLimitCoreInfo(Integer uid); // 1468

    void sendGMCommand(Integer uid, byte[] type, byte[] cmd); // 2001

    GMCommandResultDTO getGMCommandResult(Integer uid); // 2000

    RoleDTO getOtherRoleInfo(Integer otherUid); // 1462/1463


    UserDetails loadUserByUsername(String username);
}