package com.SouthMillion.config_service.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class UserProtocolService {

    public String getPrivacyNotice() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("privacy_notice.txt")) {
            if (is == null) return "Privacy notice not found!";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error reading privacy notice!";
        }
    }
}