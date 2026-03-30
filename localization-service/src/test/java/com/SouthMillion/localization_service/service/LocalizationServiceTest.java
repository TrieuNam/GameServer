package com.SouthMillion.localization_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalizationService Tests")
class LocalizationServiceTest {

    @Mock private RedisTemplate<String, Object>   redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks private LocalizationService localizationService;

    // =========================================================
    // translate
    // =========================================================
    @Nested
    @DisplayName("translate()")
    class Translate {

        @Test
        @DisplayName("TC-L10N-001 [P] Cache hit – tra ve ban dich tu cache")
        void translate_cacheHit_returnsCached() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("i18n:en:welcome")).willReturn("Welcome to LineR Game!");

            String result = localizationService.translate("welcome", "en");

            assertThat(result).isEqualTo("Welcome to LineR Game!");
            then(valueOps).should(never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("TC-L10N-002 [P] Cache miss tieng Anh – tra ve va luu cache")
        void translate_cacheMiss_english_returnsAndCaches() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("i18n:en:level_up")).willReturn(null);

            String result = localizationService.translate("level_up", "en");

            assertThat(result).isEqualTo("Level Up!");
            then(valueOps).should().set(eq("i18n:en:level_up"), eq("Level Up!"), anyLong(), any());
        }

        @Test
        @DisplayName("TC-L10N-003 [P] Cache miss tieng Viet – tra ve ban dich tieng Viet")
        void translate_cacheMiss_vietnamese_returnsViTranslation() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("i18n:vi:battle_win")).willReturn(null);

            String result = localizationService.translate("battle_win", "vi");

            assertThat(result).isEqualTo("Chiến Thắng!");
        }

        @Test
        @DisplayName("TC-L10N-004 [P] Ngon ngu khong ho tro – fallback sang tieng Anh")
        void translate_unsupportedLanguage_fallbackToEnglish() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("i18n:fr:welcome")).willReturn(null);

            String result = localizationService.translate("welcome", "fr");

            assertThat(result).isEqualTo("Welcome to LineR Game!");
        }

        @Test
        @DisplayName("TC-L10N-005 [P] Key khong ton tai – tra ve chinh key do")
        void translate_missingKey_returnsKeyAsDefault() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get("i18n:en:unknown_key")).willReturn(null);

            String result = localizationService.translate("unknown_key", "en");

            assertThat(result).isEqualTo("unknown_key");
        }
    }

    // =========================================================
    // getAll
    // =========================================================
    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("TC-L10N-006 [P] Lay tat ca ban dich tieng Trung")
        void getAll_chinese_returnsChinese() {
            Map<String, String> result = localizationService.getAll("zh");

            assertThat(result).isNotEmpty();
            assertThat(result.get("welcome")).isEqualTo("欢迎来到LineR游戏!");
        }

        @Test
        @DisplayName("TC-L10N-007 [P] Ngon ngu khong ho tro – fallback sang tieng Anh")
        void getAll_unsupportedLanguage_returnsEnglish() {
            Map<String, String> result = localizationService.getAll("jp");

            assertThat(result).containsKey("welcome");
            assertThat(result.get("battle_win")).isEqualTo("Victory!");
        }
    }
}
