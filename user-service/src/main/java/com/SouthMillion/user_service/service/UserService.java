package com.SouthMillion.user_service.service;


import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo repo;
    private final PasswordEncoder encoder;

    public User createUser(String account, String username, String rawPassword) {
        // Kiểm tra trùng
        repo.findByAccount(account).ifPresent(u -> {
            throw new IllegalArgumentException("Account already exists");
        });
        repo.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists");
        });

        User u = User.builder()
                .userId(UUID.randomUUID().toString())
                .account(account)
                .username(username)
                .passHash(encoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .build();
        return repo.save(u);
    }

    public Optional<User> findById(String userId) {
        return repo.findById(userId);
    }

    public Optional<User> findByAccountOrUsername(String s) {
        return repo.findByAccount(s).or(() -> repo.findByUsername(s));
    }
}