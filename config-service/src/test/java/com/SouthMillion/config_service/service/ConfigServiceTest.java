package com.SouthMillion.config_service.service;

import com.SouthMillion.config_service.config.ConfigProps;
import com.SouthMillion.config_service.config.cache.CacheTier;
import com.SouthMillion.config_service.core.ClasspathConfigRepository;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService Tests")
class ConfigServiceTest {

    @Mock private CacheTier cache;
    @Mock private ClasspathConfigRepository repo;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        // ConfigProps is a record – construct directly with mode = "classpath"
        ConfigProps props = new ConfigProps("classpath", null, null, null, null, null, null, null);
        configService = new ConfigService(List.of(repo), cache, props);
    }

    // =========================================================
    // get()
    // =========================================================
    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("TC-CFG-001 [N] Path null – tra ve Optional rong")
        void get_nullPath_returnsEmpty() {
            Optional<ConfigEnvelope> result = configService.get(null);
            assertThat(result).isEmpty();
            then(repo).should(never()).read(any());
        }

        @Test
        @DisplayName("TC-CFG-002 [N] Path trang – tra ve Optional rong")
        void get_blankPath_returnsEmpty() {
            Optional<ConfigEnvelope> result = configService.get("   ");
            assertThat(result).isEmpty();
            then(repo).should(never()).read(any());
        }

        @Test
        @DisplayName("TC-CFG-003 [P] Cache hit – tra ve gia tri tu cache ma khong goi repo")
        void get_cacheHit_returnsCachedValue() {
            ConfigEnvelope env = mock(ConfigEnvelope.class);
            given(cache.get("some/path")).willReturn(Optional.of(env));

            Optional<ConfigEnvelope> result = configService.get("some/path");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(env);
            then(repo).should(never()).read(any());
        }

        @Test
        @DisplayName("TC-CFG-004 [P] Cache miss – doc tu repo va luu vao cache")
        void get_cacheMiss_readsFromRepoAndCaches() {
            ConfigEnvelope env = mock(ConfigEnvelope.class);
            given(cache.get("config/map")).willReturn(Optional.empty());
            given(repo.read("config/map")).willReturn(Optional.of(env));

            Optional<ConfigEnvelope> result = configService.get("config/map");

            assertThat(result).isPresent();
            then(cache).should().put("config/map", env);
        }

        @Test
        @DisplayName("TC-CFG-005 [N] Cache miss va repo khong tim thay – tra ve Optional rong, khong luu cache")
        void get_cacheMissAndRepoEmpty_returnsEmpty() {
            given(cache.get("not/exist")).willReturn(Optional.empty());
            given(repo.read("not/exist")).willReturn(Optional.empty());

            Optional<ConfigEnvelope> result = configService.get("not/exist");

            assertThat(result).isEmpty();
            then(cache).should(never()).put(any(), any());
        }
    }

    // =========================================================
    // exists()
    // =========================================================
    @Nested
    @DisplayName("exists()")
    class Exists {

        @Test
        @DisplayName("TC-CFG-006 [P] Path ton tai – tra ve true")
        void exists_found_returnsTrue() {
            given(repo.exists("config/exist")).willReturn(true);
            assertThat(configService.exists("config/exist")).isTrue();
        }

        @Test
        @DisplayName("TC-CFG-007 [N] Path khong ton tai – tra ve false")
        void exists_notFound_returnsFalse() {
            given(repo.exists("config/miss")).willReturn(false);
            assertThat(configService.exists("config/miss")).isFalse();
        }
    }

    // =========================================================
    // list()
    // =========================================================
    @Nested
    @DisplayName("list()")
    class ListPrefix {

        @Test
        @DisplayName("TC-CFG-008 [P] List theo prefix – tra ve danh sach paths")
        void list_returnsMatchingPaths() {
            given(repo.list("config/")).willReturn(List.of("config/a", "config/b"));

            List<String> result = configService.list("config/");

            assertThat(result).containsExactlyInAnyOrder("config/a", "config/b");
        }

        @Test
        @DisplayName("TC-CFG-009 [P] Khong co file nao – tra ve danh sach rong")
        void list_noFiles_returnsEmpty() {
            given(repo.list("empty/")).willReturn(List.of());

            List<String> result = configService.list("empty/");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // evict()
    // =========================================================
    @Nested
    @DisplayName("evict()")
    class Evict {

        @Test
        @DisplayName("TC-CFG-010 [P] Xoa cache theo path")
        void evict_callsCacheEvict() {
            configService.evict("config/remove");
            then(cache).should().evict("config/remove");
        }
    }
}
