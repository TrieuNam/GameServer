package com.SouthMillion.user_service.service.impl;

import com.SouthMillion.user_service.enity.RoleEntity;
import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.mapper.RoleMapper;
import com.SouthMillion.user_service.repository.RoleRepository;
import com.SouthMillion.user_service.repository.UserRepository;
import com.SouthMillion.user_service.service.UserGameService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.SouthMillion.dto.user.*;
import org.SouthMillion.dto.user.request.LoginRequestDTO;
import org.SouthMillion.dto.user.request.RegisterRequestDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.dto.user.response.RegisterResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserGameServiceImpl implements UserGameService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ---- 7000/7056: LOGIN ----
    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
        // Tìm theo pname
        Optional<RoleEntity> entityOpt = roleRepository.findByUsername(req.getUsername());
        LoginResponseDTO resp = new LoginResponseDTO();

        if (entityOpt.isPresent()) {
            RoleEntity user = entityOpt.get();
            if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                resp.setResult(0); // ok
                resp.setMessage("Login successful!");
                resp.setForbidTime(0);
            } else {
                resp.setResult(1); // sai password
                resp.setMessage("Invalid password!");
                resp.setForbidTime(0);
            }
        } else {
            // Nếu chưa tồn tại thì auto-register (tạo mới)
            RoleEntity entity = new RoleEntity();
            entity.setUid(genUid());
            entity.setUsername(req.getUsername());
            entity.setCurExp(0L);
            entity.setCreateTime(System.currentTimeMillis() / 1000);
            // Lưu password đã hash
            entity.setPassword(passwordEncoder.encode(req.getPassword()));
            // RoleInfo
            RoleInfoDTO info = new RoleInfoDTO();
            info.setRoleId(entity.getUid().intValue());
            info.setName(req.getRoleName() != null ? req.getRoleName() : req.getUsername());
            info.setLevel(1);
            info.setCap(0L);
            info.setHeadPicId(1);
            info.setTitleId(0);
            info.setGuildName("");
            info.setKnightLevel(0);
            info.setHeadChar("");
            entity.setRoleinfoJson(writeJson(info));
            // Appearance
            AppearanceDTO app = new AppearanceDTO();
            app.setSurfaceWeapon(0);
            app.setSurfaceShield(0);
            app.setSurfaceBody(0);
            app.setSurfaceMount(0);
            app.setSurfaceHead(0);
            app.setSurfaceAngel(0);
            entity.setAppearanceJson(writeJson(app));
            roleRepository.save(entity);

            resp.setResult(0); // Đăng nhập thành công sau khi tạo mới
            resp.setForbidTime(0);
        }
        return resp;
    }

    // ---- 1400: ROLE INFO ----
    @Override
    public RoleDTO getRoleInfo(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        return RoleMapper.fromEntity(entity);
    }

    // ---- 1401: ROLE ATTR LIST ----
    @Override
    public RoleAttrListDTO getRoleAttrList(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        List<AttrPairDTO> attrList = readListJson(entity.getAttrListJson(), new TypeReference<List<AttrPairDTO>>() {});
        RoleAttrListDTO result = new RoleAttrListDTO();
        result.setNotifyReason(1);
        result.setCapability(calcCapability(attrList));
        result.setAttrList(attrList != null ? attrList : new ArrayList<>());
        return result;
    }

    // ---- 1402: EXP CHANGE ----
    @Override
    @Transactional
    public RoleExpChangeDTO changeExp(Integer uid, long expChange) {
        RoleEntity entity = getEntityByUid(uid);
        long oldExp = entity.getCurExp() == null ? 0 : entity.getCurExp();
        long newExp = oldExp + expChange;
        entity.setCurExp(newExp);
        roleRepository.save(entity);

        RoleExpChangeDTO dto = new RoleExpChangeDTO();
        dto.setChangeExp(expChange);
        dto.setCurExp(newExp);
        return dto;
    }

    // ---- 1403: LEVEL UP ----
    @Override
    @Transactional
    public RoleLevelChangeDTO levelUp(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        RoleInfoDTO roleinfo = readJson(entity.getRoleinfoJson(), RoleInfoDTO.class);
        if (roleinfo == null) roleinfo = new RoleInfoDTO();
        Integer oldLevel = roleinfo.getLevel() == null ? 1 : roleinfo.getLevel();
        Integer newLevel = oldLevel + 1;
        roleinfo.setLevel(newLevel);
        entity.setRoleinfoJson(writeJson(roleinfo));
        roleRepository.save(entity);

        RoleLevelChangeDTO dto = new RoleLevelChangeDTO();
        dto.setLevel(newLevel);
        dto.setExp(entity.getCurExp());
        return dto;
    }

    // ---- 1460: SET SYSTEM SETTING ----
    @Override
    @Transactional
    public void setSystemSettings(Integer uid, List<SystemSetDTO> systemSetList) {
        RoleEntity entity = getEntityByUid(uid);
        entity.setSystemSetJson(writeJson(systemSetList));
        roleRepository.save(entity);
    }

    // ---- 1461: GET SYSTEM SETTING ----
    @Override
    public RoleSystemSetDTO getSystemSettings(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        List<SystemSetDTO> systemSetList = readListJson(entity.getSystemSetJson(), new TypeReference<List<SystemSetDTO>>() {});
        RoleSystemSetDTO dto = new RoleSystemSetDTO();
        dto.setSystemSetList(systemSetList != null ? systemSetList : Collections.emptyList());
        return dto;
    }

    // ---- 1464: GET NOTICE TIME ----
    @Override
    public long getNoticeTime(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        return entity.getNoticeTime() != null ? entity.getNoticeTime() : 0L;
    }

    // ---- 1465: SET NOTICE TIME ----
    @Override
    @Transactional
    public void setNoticeTime(Integer uid, long time) {
        RoleEntity entity = getEntityByUid(uid);
        entity.setNoticeTime(time);
        roleRepository.save(entity);
    }

    // ---- 1466: SEND CMD TO CLIENT (LOGIC Ở websocket-server) ----
    @Override
    public void sendCmdToClient(Integer uid, int cmdId, byte[] str) {
        // Thường sẽ gửi Kafka/event/websocket - ở đây chỉ log lại
        System.out.println("Send cmd to client: uid=" + uid + ", cmdId=" + cmdId);
    }

    // ---- 1467: LIMIT CORE OP ----
    @Override
    @Transactional
    public void limitCoreOp(Integer uid, int type, int p1) {
        // Logic giới hạn core, ví dụ tăng cấp, random,... cập nhật lại coreLevel
        RoleEntity entity = getEntityByUid(uid);
        List<Integer> coreLevel = readListJson(entity.getCoreLevelJson(), new TypeReference<List<Integer>>() {});
        if (coreLevel == null) coreLevel = new ArrayList<>();
        if (type == 0) { // upgrade
            coreLevel.add(p1);
        }
        entity.setCoreLevelJson(writeJson(coreLevel));
        roleRepository.save(entity);
    }

    // ---- 1468: LIMIT CORE INFO ----
    @Override
    public LimitCoreInfoDTO getLimitCoreInfo(Integer uid) {
        RoleEntity entity = getEntityByUid(uid);
        List<Integer> coreLevel = readListJson(entity.getCoreLevelJson(), new TypeReference<List<Integer>>() {});
        LimitCoreInfoDTO dto = new LimitCoreInfoDTO();
        dto.setCoreLevel(coreLevel != null ? coreLevel : Collections.emptyList());
        return dto;
    }

    // ---- 2001: GM COMMAND ----
    @Override
    public void sendGMCommand(Integer uid, byte[] type, byte[] cmd) {
        // Thực tế sẽ lưu command hoặc gửi tới GM service
        System.out.println("GM Command uid=" + uid);
    }

    // ---- 2000: GM COMMAND RESULT ----
    @Override
    public GMCommandResultDTO getGMCommandResult(Integer uid) {
        GMCommandResultDTO dto = new GMCommandResultDTO();
        // Thực tế lấy từ log kết quả
        dto.setType(new byte[]{});
        dto.setResult(new byte[]{});
        return dto;
    }

    // ---- 1462/1463: LẤY INFO NGƯỜI CHƠI KHÁC ----
    @Override
    public RoleDTO getOtherRoleInfo(Integer otherUid) {
        return getRoleInfo(otherUid);
    }


    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = repo.findByUsername(username).orElseThrow();
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }


    // ===== Helper =====

    private RoleEntity getEntityByUid(Integer uid) {
        return roleRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found: " + uid));
    }

    private String writeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception ex) { return null; }
    }

    private <T> T readJson(String json, Class<T> clazz) {
        try { return objectMapper.readValue(json, clazz); } catch (Exception ex) { return null; }
    }

    private <T> T readListJson(String json, TypeReference<T> type) {
        try { return objectMapper.readValue(json, type); } catch (Exception ex) { return null; }
    }

    private Long genUid() {
        return System.currentTimeMillis() + (long)(Math.random()*10000);
    }

    private long calcCapability(List<AttrPairDTO> attrList) {
        if (attrList == null) return 0;
        return attrList.stream().mapToLong(AttrPairDTO::getAttrValue).sum();
    }
}