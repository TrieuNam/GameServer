package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.dto.requestDto.LoginRequest;
import com.SouthMillion.user_service.dto.requestDto.RegisterRequest;
import com.SouthMillion.user_service.dto.responseDto.AuthResponse;
import com.SouthMillion.user_service.dto.responseDto.RegisterResponse;
import com.SouthMillion.user_service.model.User;
import com.SouthMillion.user_service.service.MyUserDetailsService;
import jakarta.validation.Valid;
import org.SouthMillion.dto.user.*;
import org.SouthMillion.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    MyUserDetailsService userService;

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest req) {
        return userService.register(req);
    }

    @GetMapping("/login")
    public LoginVerify login(
            @RequestParam("spid") String spid,
            @RequestParam("device") String device,
            @RequestParam("userId") String userId,
            @RequestParam("timestamp") Long timestamp,
            @RequestParam(value = "sign", required = false) String sign
    ) {
        return userService.verifyLogin(spid, device, userId, timestamp, sign);
    }

    @PostMapping("/login/social")
    public String socialLogin(@RequestBody SocialLoginDTO dto, @RequestHeader("Device-Id") String deviceId) {
        return userService.socialLogin(dto, deviceId);
    }
    @PostMapping("/payment/callback")
    public void paymentCallback(@RequestBody PaymentCallbackDTO dto) {
        userService.paymentCallback(dto);
    }
    @PostMapping("/ads/reward")
    public boolean verifyAdReward(@RequestBody AdRewardDTO dto) {
        return userService.verifyAdReward(dto);
    }

}