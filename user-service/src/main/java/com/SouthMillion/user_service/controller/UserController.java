package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.model.User;
import com.SouthMillion.user_service.repository.UserRepository;
import com.SouthMillion.user_service.service.UserSettingsService;
import org.SouthMillion.dto.battle.UserDto;
import org.SouthMillion.dto.user.UserSettingsDto;
import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserSettingsService settingsService;

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        User user = repo.findById(id).orElseThrow();
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }

    @PostMapping("/settings")
    public void updateSettings(@RequestBody UserSettingsDto dto) {
        settingsService.updateSettings(dto);
    }

    @GetMapping("/settings")
    public UserSettingsDto getSettings(@RequestParam String userKey) {
        return settingsService.getSettings(userKey);
    }
}