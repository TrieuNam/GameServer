package com.SouthMillion.user_service.service;


import com.SouthMillion.user_service.config.AppProperties;
import com.SouthMillion.user_service.config.IdGenerator;
import com.SouthMillion.user_service.enity.UserAccount;
import com.SouthMillion.user_service.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.H5VerifyRequest;
import org.SouthMillion.dto.user.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repo;
    private final PasswordEncoder encoder;
    private final IdGenerator idGen;
    private final AppProperties props;

    public Optional<UserAccount> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    public boolean verifyPassword(UserAccount ua, String rawPwd) {
        return encoder.matches(rawPwd, ua.getPasswordHash());
    }

    @Transactional
    public UserAccount register(RegisterRequest req) {
        if (repo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("username exists");
        }
        UserAccount ua = UserAccount.builder()
                .id(idGen.newUserId())
                .username(req.getUsername())
                .passwordHash(encoder.encode(req.getPassword()))
                .spid(req.getSpid())
                .device(req.getDevice())
                .status("ACTIVE")
                .build();
        return repo.save(ua);
    }

    /** H5 verify + auto-provision user nếu chưa có. */
    @Transactional
    public UserAccount verifyH5AndProvision(H5VerifyRequest req) {
        // 1) check timestamp skew
        long ts = Long.parseLong(req.getTimestamp());
        long now = Instant.now().getEpochSecond();
        long skew = Math.abs(now - ts);
        if (skew > props.getH5().getSkewSeconds()) {
            throw new IllegalArgumentException("timestamp skew too large");
        }
        // 2) compute HMAC
        String secret = props.getH5().getChannels().get(req.getSpid());
        if (secret == null) throw new IllegalArgumentException("unknown spid");
        String payload = req.getSpid() + "|" + req.getDevice() + "|" + req.getUserId() + "|" + req.getTimestamp();
        String expect = hmacSha256Hex(payload, secret);
        if (!expect.equalsIgnoreCase(req.getSign())) {
            throw new IllegalArgumentException("invalid sign");
        }
        // 3) try find by spid+externalId
        Optional<UserAccount> existed = repo.findBySpidAndExternalId(req.getSpid(), req.getUserId());
        if (existed.isPresent()) {
            UserAccount ua = existed.get();
            ua.setLastLoginAt(Instant.now());
            return repo.save(ua);
        }
        // 4) create user (username generate từ spid-userId)
        String generatedName = (req.getSpid() + "_" + req.getUserId()).toLowerCase();
        String placeholderPwd = req.getSpid() + ":" + req.getUserId(); // không dùng để login password, chỉ chống trống
        UserAccount ua = UserAccount.builder()
                .id(idGen.newUserId())
                .username(generatedName)
                .passwordHash(encoder.encode(placeholderPwd))
                .spid(req.getSpid())
                .externalId(req.getUserId())
                .device(req.getDevice())
                .status("ACTIVE")
                .lastLoginAt(Instant.now())
                .build();
        return repo.save(ua);
    }

    public void markLogin(String userId) {
        repo.findById(userId).ifPresent(u -> { u.setLastLoginAt(Instant.now()); repo.save(u); });
    }

    public Map<String,Object> toVerifyResp(UserAccount u) {
        return Map.of(
                "ok", true,
                "userId", u.getId(),
                "username", u.getUsername(),
                "status", u.getStatus()
        );
    }

    private static String hmacSha256Hex(String msg, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }
}