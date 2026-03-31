package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.UserStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;

    /**
     * Verify password cho session-service.
     * Trả về User nếu ok, null nếu sai mật khẩu/không ACTIVE.
     */
    public User verifyPassword(String accountOrUsername, String rawPassword) {
        var user = userRepo.findByAccount(accountOrUsername)
                .or(() -> userRepo.findByUsername(accountOrUsername))
                .orElse(null);
        if (user == null) return null;
        if (user.getStatus() != UserStatus.ACTIVE) return null;
        return encoder.matches(rawPassword, user.getPassHash()) ? user : null;
    }

    /**
     * Kiểm tra trạng thái active, cache 30s (Redis).
     */
    @Cacheable(value = "userActive", key = "#userId")
    public boolean isActive(String userId) {
        return userRepo.findById(userId)
                .map(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }
}