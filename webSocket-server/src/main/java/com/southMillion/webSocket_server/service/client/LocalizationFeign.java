package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "localization-service")
public interface LocalizationFeign {
    
    @GetMapping("/api/i18n/translate")
    Map<String, Object> translate(@RequestParam String key, @RequestParam String lang);
    
    @GetMapping("/api/i18n/all/{language}")
    Map<String, String> getAll(@PathVariable("language") String language);
}
