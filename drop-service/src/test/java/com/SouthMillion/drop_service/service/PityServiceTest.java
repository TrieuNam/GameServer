package com.SouthMillion.drop_service.service;

import com.SouthMillion.drop_service.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("PityService Tests")
class PityServiceTest {

    @Mock private StringRedisTemplate                redis;
    @Mock private AppProperties                      props;
    @Mock private ValueOperations<String, String>    valueOps;
    @Mock private AppProperties.Pity                 pityConfig;

    @InjectMocks private PityService pityService;

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
        given(props.getPity()).willReturn(pityConfig);
    }

    // =========================================================
    // incr()
    // =========================================================
    @Nested
    @DisplayName("incr()")
    class Incr {

        @Test
        @DisplayName("TC-PITY-001 [P] Tang bien dem pity – tra ve gia tri moi")
        void incr_returnsIncrementedValue() {
            given(valueOps.increment("drop:pity:group1:role1")).willReturn(5L);

            int result = pityService.incr("group1", "role1");

            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-PITY-002 [N] Redis tra ve null – tra ve 0")
        void incr_redisNull_returnsZero() {
            given(valueOps.increment("drop:pity:group1:role1")).willReturn(null);

            int result = pityService.incr("group1", "role1");

            assertThat(result).isEqualTo(0);
        }
    }

    // =========================================================
    // get()
    // =========================================================
    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("TC-PITY-003 [P] Lay bien dem pity hien tai")
        void get_returnsCurrentCounter() {
            given(valueOps.get("drop:pity:grp:r99")).willReturn("42");

            int result = pityService.get("grp", "r99");

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("TC-PITY-004 [N] Khoa chua ton tai – tra ve 0")
        void get_keyNotFound_returnsZero() {
            given(valueOps.get("drop:pity:grp:new")).willReturn(null);

            int result = pityService.get("grp", "new");

            assertThat(result).isEqualTo(0);
        }
    }

    // =========================================================
    // reset()
    // =========================================================
    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("TC-PITY-005 [P] Reset bien dem pity – xoa khoa Redis")
        void reset_deletesRedisKey() {
            pityService.reset("grp", "role1");

            then(redis).should().delete("drop:pity:grp:role1");
        }
    }

    // =========================================================
    // enabled()
    // =========================================================
    @Nested
    @DisplayName("enabled()")
    class Enabled {

        @Test
        @DisplayName("TC-PITY-006 [P] Pity duoc bat – tra ve true")
        void enabled_whenTrue_returnsTrue() {
            given(pityConfig.isEnabled()).willReturn(true);
            assertThat(pityService.enabled()).isTrue();
        }

        @Test
        @DisplayName("TC-PITY-007 [N] Pity bi tat – tra ve false")
        void enabled_whenFalse_returnsFalse() {
            given(pityConfig.isEnabled()).willReturn(false);
            assertThat(pityService.enabled()).isFalse();
        }
    }

    // =========================================================
    // thresholdFor()
    // =========================================================
    @Nested
    @DisplayName("thresholdFor()")
    class ThresholdFor {

        @Test
        @DisplayName("TC-PITY-008 [P] Co cau hinh rieng cho dropId – tra ve threshold rieng")
        void thresholdFor_perDropConfig_returnsSpecific() {
            AppProperties.Pity.PerDrop perDrop = mock(AppProperties.Pity.PerDrop.class);
            given(perDrop.getThreshold()).willReturn(30);
            Map<String, AppProperties.Pity.PerDrop> map = new HashMap<>();
            map.put("10", perDrop);
            given(pityConfig.getPerDrop()).willReturn(map);

            int result = pityService.thresholdFor(10);

            assertThat(result).isEqualTo(30);
        }

        @Test
        @DisplayName("TC-PITY-009 [N] Khong co cau hinh rieng – tra ve threshold mac dinh")
        void thresholdFor_noPerDropConfig_returnsDefault() {
            given(pityConfig.getPerDrop()).willReturn(new HashMap<>());
            given(pityConfig.getDefaultThreshold()).willReturn(50);

            int result = pityService.thresholdFor(99);

            assertThat(result).isEqualTo(50);
        }
    }

    // =========================================================
    // rareSelectorFor()
    // =========================================================
    @Nested
    @DisplayName("rareSelectorFor()")
    class RareSelectorFor {

        @Test
        @DisplayName("TC-PITY-010 [P] Co cau hinh selector rieng – tra ve selector rieng")
        void rareSelectorFor_perDropConfig_returnsSpecific() {
            AppProperties.Pity.PerDrop perDrop = mock(AppProperties.Pity.PerDrop.class);
            given(perDrop.getRareSelector()).willReturn("LIST");
            Map<String, AppProperties.Pity.PerDrop> map = new HashMap<>();
            map.put("5", perDrop);
            given(pityConfig.getPerDrop()).willReturn(map);

            String result = pityService.rareSelectorFor(5);

            assertThat(result).isEqualTo("LIST");
        }

        @Test
        @DisplayName("TC-PITY-011 [N] Khong co cau hinh – tra ve selector mac dinh")
        void rareSelectorFor_noConfig_returnsDefault() {
            given(pityConfig.getPerDrop()).willReturn(new HashMap<>());
            given(pityConfig.getDefaultRareSelector()).willReturn("BROADCAST");

            String result = pityService.rareSelectorFor(99);

            assertThat(result).isEqualTo("BROADCAST");
        }
    }

    // =========================================================
    // rareListFor()
    // =========================================================
    @Nested
    @DisplayName("rareListFor()")
    class RareListFor {

        @Test
        @DisplayName("TC-PITY-012 [P] Co danh sach item hiem – tra ve danh sach")
        void rareListFor_perDropConfig_returnsList() {
            AppProperties.Pity.PerDrop perDrop = mock(AppProperties.Pity.PerDrop.class);
            given(perDrop.getRareItemIds()).willReturn(List.of(100, 101, 102));
            Map<String, AppProperties.Pity.PerDrop> map = new HashMap<>();
            map.put("7", perDrop);
            given(pityConfig.getPerDrop()).willReturn(map);

            List<Integer> result = pityService.rareListFor(7);

            assertThat(result).containsExactly(100, 101, 102);
        }

        @Test
        @DisplayName("TC-PITY-013 [N] Khong co cau hinh – tra ve danh sach rong")
        void rareListFor_noConfig_returnsEmpty() {
            given(pityConfig.getPerDrop()).willReturn(new HashMap<>());

            List<Integer> result = pityService.rareListFor(99);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // groupOf()
    // =========================================================
    @Nested
    @DisplayName("groupOf()")
    class GroupOf {

        @Test
        @DisplayName("TC-PITY-014 [P] Co override – tra ve override")
        void groupOf_withOverride_returnsOverride() {
            String result = pityService.groupOf(10, "custom-group");
            assertThat(result).isEqualTo("custom-group");
        }

        @Test
        @DisplayName("TC-PITY-015 [N] Khong co override – tra ve group mac dinh theo dropId")
        void groupOf_noOverride_returnsDefaultGroup() {
            String result = pityService.groupOf(42, null);
            assertThat(result).isEqualTo("drop:42");
        }
    }
}
