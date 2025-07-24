package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.model.Role;
import com.SouthMillion.user_service.model.User;
import com.SouthMillion.user_service.repository.RoleRepository;
import com.SouthMillion.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmUserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void banUser(String userId) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setStatus("banded");
        userRepository.save(user);
    }

    public void unbanUser(String userId) {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found!"));
        user.setStatus("active");
        userRepository.save(user);
    }

    public void resetUser(String userId) {
        Role role = roleRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found!"));
        // reset logic, ví dụ reset exp, level về mặc định
        role.setLevel(1);
        role.setExp(0L);
        role.setVip("0");
        // Có thể reset thêm field khác nếu muốn
        roleRepository.save(role);
    }
}