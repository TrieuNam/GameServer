package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.dto.requestDto.RegisterRequest;
import com.SouthMillion.user_service.dto.responseDto.RegisterResponse;
import com.SouthMillion.user_service.model.ActivityLog;
import com.SouthMillion.user_service.model.Role;
import com.SouthMillion.user_service.model.User;
import com.SouthMillion.user_service.repository.ActivityLogRepository;
import com.SouthMillion.user_service.repository.RoleRepository;
import com.SouthMillion.user_service.repository.UserRepository;
import com.SouthMillion.user_service.utils.AdVerifier;
import com.SouthMillion.user_service.utils.PaymentVerifier;
import com.SouthMillion.user_service.utils.SocialVerifier;
import jakarta.transaction.Transactional;
import org.SouthMillion.dto.user.*;
import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    ActivityLogRepository logRepo;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    private SocialVerifier socialVerifier;

    private final PasswordEncoder passwordEncoder;

    public MyUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = repo.findByUsername(username).orElseThrow();
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public LoginVerify verifyLogin(String spid, String device, String userId, Long timestamp, String sign) {
        LoginVerify result = new LoginVerify();
        Optional<User> userOpt = repo.findByUsername(userId);
        if (userOpt.isEmpty()) {
            result.setRet(1); // user not found
            return result;
        }
        User user = userOpt.get();
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            result.setRet(2); // user blocked
            return result;
        }
        user.setLastLogin(LocalDateTime.now());
        user.setDeviceId(device);
        repo.save(user);

        String jwt = jwtUtil.generateToken(user.getId().toString(), user.getUsername(), user.getEmail());

        // Lấy role list
        List<Role> userRoles = roleRepository.findByUser(user);
        List<ResponseServerData> roleData = new ArrayList<>();
        for (Role role : userRoles) {
            ResponseServerData s = new ResponseServerData();
            s.setLast_login_time(role.getLastLoginTime() != null ? role.getLastLoginTime() : 0L);
            s.setLevel(role.getLevel());
            s.setRole_id(role.getRoleId());
            s.setRole_name(role.getRoleName());
            s.setServer_id(role.getServerId());
            s.setVip(role.getVip());
            roleData.add(s);
        }

        // Build UserInfo
        ResponseUserInfo userInfo = new ResponseUserInfo();
        userInfo.setRole_data(roleData);
        userInfo.setAccount(user.getUsername());
        userInfo.setAccount_type(user.getAccountType() != null ? user.getAccountType() : 0);
        userInfo.setFcm_flag(user.getFcmFlag() != null ? user.getFcmFlag() : 0);
        userInfo.setLogin_sign(jwt);
        userInfo.setLogin_time(System.currentTimeMillis() / 1000L);
        userInfo.setUid(user.getId().toString());
        userInfo.setOpenid(user.getOpenid() != null ? user.getOpenid() : "");
        userInfo.setMerger_spid(spid);

        result.setRet(0);
        result.setUser(userInfo);
        result.setRole_data(roleData);
        return result;
    }


    public RegisterResponse register(RegisterRequest req) {
        RegisterResponse resp = new RegisterResponse();
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            resp.setRet(1);
            resp.setMessage("Username already exists");
            return resp;
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setAccountType(req.getAccountType() != null ? req.getAccountType() : 1);
        user.setFcmFlag(req.getFcmFlag() != null ? req.getFcmFlag() : 0);
        user.setOpenid(req.getOpenid());
        user.setStatus("active");
        user = repo.save(user);

        resp.setRet(0);
        resp.setMessage("Register success");
        resp.setUserId(user.getId());
        return resp;
    }

    public void logActivity(Long userId, String action, String data) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setData(data);
        log.setCreatedAt(LocalDateTime.now());
        logRepo.save(log);
    }

    public String socialLogin(SocialLoginDTO dto, String deviceId) {
        User user = null;

        if ("google".equalsIgnoreCase(dto.getProvider())) {
            GoogleProfile profile = socialVerifier.verifyGoogle(dto.getToken());
            if (profile == null || profile.getSub() == null)
                throw new RuntimeException("Invalid Google token");

            Optional<User> userOpt = repo.findByGoogleId(profile.getSub());
            user = userOpt.orElseGet(() -> {
                User newUser = new User();
                newUser.setGoogleId(profile.getSub());
                newUser.setEmail(profile.getEmail());
                newUser.setNickname(profile.getName());
                newUser.setAvatar(profile.getPicture());
                newUser.setLanguage(profile.getLocale());
                newUser.setUsername("gg_" + profile.getSub());
                newUser.setStatus("active");
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setPassword("SOCIAL_LOGIN_" + UUID.randomUUID());
                newUser.setCoin(0);
                newUser.setGem(0);
                return newUser;
            });

            if (!"active".equalsIgnoreCase(user.getStatus()))
                throw new RuntimeException("User blocked");
            user.setUsername(profile.getName());
            user.setLastLogin(LocalDateTime.now());
            user.setDeviceId(deviceId);
            user.setUpdatedAt(LocalDateTime.now());
            repo.save(user);

        } else {
            throw new RuntimeException("Provider not supported");
        }

        logActivity(user.getId(), "social_login", dto.getProvider() + "/" + deviceId);
        return jwtUtil.generateToken(user.getId().toString(), user.getUsername(), user.getEmail());
    }



    public void paymentCallback(PaymentCallbackDTO dto) {
        if (!PaymentVerifier.verify(dto.getTransactionId(), dto.getUserId(), dto.getAmount()))
            throw new RuntimeException("Invalid transaction");
        User user = repo.findById(dto.getUserId()).orElseThrow();
        user.setGem(user.getGem() + dto.getAmount());
        repo.save(user);
        logActivity(user.getId(), "payment_callback", dto.getTransactionId());
    }

    public boolean verifyAdReward(AdRewardDTO dto) {
        if (!AdVerifier.verify(dto.getAdProvider(), dto.getAdToken())) return false;
        User user = repo.findById(dto.getUserId()).orElseThrow();
        user.setCoin(user.getCoin() + 10);
        repo.save(user);
        logActivity(user.getId(), "ads_reward", dto.getAdProvider());
        return true;
    }



}
