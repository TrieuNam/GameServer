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

    public User login(String accountOrUsername, String rawPassword) {
        User user = repo.findByAccount(accountOrUsername)
                .or(() -> repo.findByUsername(accountOrUsername))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User not active");
        }
        
        if (!encoder.matches(rawPassword, user.getPassHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        return user;
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!encoder.matches(oldPassword, user.getPassHash())) {
            throw new IllegalArgumentException("Invalid old password");
        }
        
        user.setPassHash(encoder.encode(newPassword));
        repo.save(user);
    }

    public void updateStatus(String userId, UserStatus newStatus) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setStatus(newStatus);
        repo.save(user);
    }
}