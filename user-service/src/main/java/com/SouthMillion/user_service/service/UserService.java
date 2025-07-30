package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.enity.*;
import com.SouthMillion.user_service.repository.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.SouthMillion.dto.user.RoleDatumDTO;
import org.SouthMillion.dto.user.RoleInfoDTO;
import org.SouthMillion.dto.user.UserDTO;
import org.SouthMillion.dto.user.UserInfoDTO;
import org.SouthMillion.dto.user.response.LoginResponseDTO;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private RoleRepository roleRepo;
    @Autowired
    private RoleAttrDetailRepository attrRepo;
    @Autowired
    private SystemSettingRepository sysRepo;
    @Autowired
    private GMCommandRepository gmRepo;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponseDTO loginCheck(String spid, String device, String userId, Long timestamp, String loginStr) {
        UserEntity user = userRepo.findByPlatUserName(userId).orElse(null);

        // 1. User không tồn tại
        if (user == null) {
            return LoginResponseDTO.builder()
                    .ret(1)
                    .msg("User not found!")
                    .build();
        }

        // 2. Check bị khoá
        if (user.getForbidTime() != null && user.getForbidTime() > (System.currentTimeMillis() / 1000)) {
            return LoginResponseDTO.builder()
                    .ret(2)
                    .msg("User is banned")
                    .build();
        }
        String token = jwtUtil.generateToken(user.getUid(), user.getPlatUserName(), null);

        // 3. Kiểm tra token/sign (bỏ comment nếu muốn dùng)
//        if (user.getToken() == null || !user.getToken().equals(loginStr)) {
//            return LoginResponseDTO.builder()
//                    .ret(1)
//                    .msg("Invalid token")
//                    .build();
//        }

        // 4. Build thông tin user
        UserDTO userDto = UserDTO.builder()
                .account(user.getPlatUserName())
                .account_type(user.getAccountType() == null ? 0 : user.getAccountType())
                .fcm_flag(user.getFcmFlag() == null ? 0 : user.getFcmFlag())
                .login_sign(token)
                .login_time(System.currentTimeMillis() / 1000)
                .uid(user.getUid())
                .openid(user.getOpenId())
                .merger_spid(spid)
                .build();

        // 5. Build role_data (giả sử user có nhiều role)
        Map<String, RoleDatumDTO> roleDataMap = new HashMap<>();
        List<RoleEntity> roleList = roleRepo.findAllByPlatUserName(userId);
        for (RoleEntity role : roleList) {
            RoleDatumDTO rd = RoleDatumDTO.builder()
                    .role_id(String.valueOf(role.getRoleId()))
                    .role_name(role.getRoleName())
                    .level(String.valueOf(role.getLevel()))
                    .last_login_time(role.getLastLoginTime())
                    .server_id(String.valueOf(role.getServerId()))
                    .vip(String.valueOf(role.getVip()))
                    .build();
            roleDataMap.put(String.valueOf(role.getServerId()), rd);
        }

        return LoginResponseDTO.builder()
                .ret(0)
                .msg("OK")
                .user(userDto)
                .role_data(roleDataMap)
                .build();
    }

    public List<Integer> getRoleIds(String platUserName) {
        return roleRepo.findAllByPlatUserName(platUserName)
                .stream()
                .map(RoleEntity::getRoleId)
                .collect(Collectors.toList());
    }

    public UserInfoDTO getAccountInfo(String platUserName) {
        UserEntity user = userRepo.findByPlatUserName(platUserName).orElse(null);
        if (user == null) return null;
        return UserInfoDTO.builder()
                .platUserName(user.getPlatUserName())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .forbidTime(user.getForbidTime())
                .build();
    }

    public RoleInfoDTO getRoleInfo(Integer roleId) {
        RoleEntity role = roleRepo.findByRoleId(roleId).orElse(null);
        RoleAttrDetailEntity attr = attrRepo.findByRoleId(roleId).orElse(null);
        if (role == null || attr == null) return null;
        return RoleInfoDTO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .level(role.getLevel())
                .createTime(role.getCreateTime())
                .exp(attr.getExp())
                .cap(attr.getCap())
                .build();
    }

    public List<Msgrole.PB_system_set> getSystemSetting(Integer roleId) {
        SystemSettingEntity setting = sysRepo.findByRoleId(roleId).orElse(null);
        if (setting == null) return Collections.emptyList();
        Type type = new TypeToken<List<Msgrole.PB_system_set>>() {
        }.getType();
        return new Gson().fromJson(setting.getSystemSetListJson(), type);
    }

    public void saveSystemSetting(Integer roleId, List<Msgrole.PB_system_set> settingList) {
        String json = new Gson().toJson(settingList);
        SystemSettingEntity entity = sysRepo.findByRoleId(roleId).orElse(new SystemSettingEntity());
        entity.setRoleId(roleId);
        entity.setSystemSetListJson(json);
        sysRepo.save(entity);
    }

    public void saveGMCommand(Integer roleId, String type, String command, String result) {
        gmRepo.save(GMCommandEntity.builder()
                .roleId(roleId)
                .commandType(type)
                .command(command)
                .result(result)
                .execTime(System.currentTimeMillis() / 1000)
                .build());
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ở đây username là platUserName hoặc userId
        UserEntity user = userRepo.findByPlatUserName(username).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Nếu bạn lưu password thật thì trả về password, còn không có thì để ""
        // GrantedAuthority: quyền, bạn có thể trả về List.of(new SimpleGrantedAuthority("USER"))
        return new org.springframework.security.core.userdetails.User(
                user.getPlatUserName(),
                user.getToken() == null ? "" : user.getToken(), // hoặc password hash
                List.of(new SimpleGrantedAuthority("USER"))
        );
    }

    public Integer getUserLevel(String userId) {
        // userId mapping với platUserName ở bảng role
        RoleEntity role = roleRepo.findAllByPlatUserName(userId)
                .stream().findFirst().orElse(null);
        return role != null ? role.getLevel() : null;
    }
}