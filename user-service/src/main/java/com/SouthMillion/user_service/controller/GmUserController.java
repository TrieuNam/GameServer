package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.GmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gm")
public class GmUserController {

    @Autowired
    private GmUserService gmUserService;

    @PostMapping("/ban")
    public void banUser(@RequestParam String userId) {
        gmUserService.banUser(userId);
    }

    @PostMapping("/unban")
    public void unbanUser(@RequestParam String userId) {
        gmUserService.unbanUser(userId);
    }

    @PostMapping("/reset")
    public void resetUser(@RequestParam String userId) {
        gmUserService.resetUser(userId);
    }
}