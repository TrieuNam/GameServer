package com.SouthMillion.localization_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalizationService {
    
    private final StringRedisTemplate redisTemplate;
    
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    
    static {
        // English
        Map<String, String> en = new HashMap<>();
        en.put("welcome", "Welcome to LineR Game!");
        en.put("level_up", "Level Up!");
        en.put("battle_win", "Victory!");
        en.put("battle_lose", "Defeat");
        TRANSLATIONS.put("en", en);
        
        // Vietnamese
        Map<String, String> vi = new HashMap<>();
        vi.put("welcome", "Chào mừng đến LineR Game!");
        vi.put("level_up", "Lên Cấp!");
        vi.put("battle_win", "Chiến Thắng!");
        vi.put("battle_lose", "Thua Cuộc");
        TRANSLATIONS.put("vi", vi);
        
        // Chinese
        Map<String, String> zh = new HashMap<>();
        zh.put("welcome", "欢迎来到LineR游戏!");
        zh.put("level_up", "升级!");
        zh.put("battle_win", "胜利!");
        zh.put("battle_lose", "失败");
        TRANSLATIONS.put("zh", zh);
    }
    
    public String translate(String key, String language) {
        String cacheKey = "i18n:" + language + ":" + key;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Map<String, String> langMap = TRANSLATIONS.getOrDefault(language, TRANSLATIONS.get("en"));
        String translation = langMap.getOrDefault(key, key);
        
        redisTemplate.opsForValue().set(cacheKey, translation, 1, TimeUnit.HOURS);
        return translation;
    }
    
    public Map<String, String> getAll(String language) {
        return TRANSLATIONS.getOrDefault(language, TRANSLATIONS.get("en"));
    }
}
