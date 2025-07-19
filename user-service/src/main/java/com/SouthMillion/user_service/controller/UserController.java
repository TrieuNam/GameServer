package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.model.User;
import com.SouthMillion.user_service.repository.UserRepository;
import org.SouthMillion.dto.UserDto;
import org.SouthMillion.dto.user.BattleStateDTO;
import org.SouthMillion.dto.user.OnlineStateDTO;
import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        User user = repo.findById(id).orElseThrow();
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }

}