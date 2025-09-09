package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;

    public User verifyPassword(String accountOrUsername, String rawPassword) {
        // Ưu tiên account; fallback username để tương thích dữ liệu/khách hàng cũ
        var user = userRepo.findByAccount(accountOrUsername)
                .or(() -> userRepo.findByUsername(accountOrUsername))
                .orElse(null);

        if (user == null) return null;
        if (user.getStatus() != UserStatus.ACTIVE) return null;
        return encoder.matches(rawPassword, user.getPassHash()) ? user : null;
    }

    public boolean isActive(String userId) {
        return userRepo.findById(userId)
                .map(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }
}