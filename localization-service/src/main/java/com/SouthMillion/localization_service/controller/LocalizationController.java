package com.SouthMillion.localization_service.controller;

import com.SouthMillion.localization_service.service.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
@RequiredArgsConstructor
public class LocalizationController {
    
    private final LocalizationService localizationService;
    
    @GetMapping("/translate")
    public ResponseEntity<Map<String, Object>> translate(
        @RequestParam String key,
        @RequestParam(defaultValue = "en") String lang
    ) {
        String translation = localizationService.translate(key, lang);
        return ResponseEntity.ok(Map.of("key", key, "lang", lang, "value", translation));
    }
    
    @GetMapping("/all/{language}")
    public ResponseEntity<Map<String, String>> getAll(@PathVariable String language) {
        return ResponseEntity.ok(localizationService.getAll(language));
    }
}
