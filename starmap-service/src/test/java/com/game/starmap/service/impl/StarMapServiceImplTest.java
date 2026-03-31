package com.game.starmap.service.impl;

import com.SouthMillion.starmap_service.client.BagClient;
import com.SouthMillion.starmap_service.client.RoleClient;
import com.SouthMillion.starmap_service.client.WalletClient;
import com.SouthMillion.starmap_service.exception.StarMapServiceException;
import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;
import com.SouthMillion.starmap_service.repository.ConstellationRepository;
import com.SouthMillion.starmap_service.repository.StarRepository;
import com.SouthMillion.starmap_service.service.impl.StarMapServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StarMapServiceImpl Tests")
class StarMapServiceImplTest {

    @Mock private StarRepository         starRepository;
    @Mock private ConstellationRepository constellationRepository;
    @Mock private BagClient              bagClient;
    @Mock private WalletClient           walletClient;
    @Mock private RoleClient             roleClient;

    @InjectMocks private StarMapServiceImpl starMapService;

    private static Star star(Long userId, Integer starId, int level, boolean active, long energy) {
        Star s = new Star();
        s.setUserId(userId);
        s.setStarId(starId);
        s.setLevel(level);
        s.setIsActive(active);
        s.setEnergy(energy);
        return s;
    }

    private static Constellation constellation(Long userId, Integer constId, int level,
                                               boolean unlocked, boolean completed,
                                               int starsActivated, int totalStars, long power) {
        Constellation c = new Constellation();
        c.setUserId(userId);
        c.setConstellationId(constId);
        c.setLevel(level);
        c.setIsUnlocked(unlocked);
        c.setIsCompleted(completed);
        c.setStarsActivated(starsActivated);
        c.setTotalStars(totalStars);
        c.setPower(power);
        return c;
    }

    // =========================================================
    // getAllStars()
    // =========================================================
    @Nested
    @DisplayName("getAllStars()")
    class GetAllStars {

        @Test
        @DisplayName("TC-STARMAP-001 [P] Lay danh sach ngoi sao cua nguoi dung")
        void getAllStars_returnsAll() {
            given(starRepository.findByUserId(1L)).willReturn(List.of(star(1L, 1, 5, true, 100L)));
            assertThat(starMapService.getAllStars(1L)).hasSize(1);
        }
    }

    // =========================================================
    // getStar()
    // =========================================================
    @Nested
    @DisplayName("getStar()")
    class GetStar {

        @Test
        @DisplayName("TC-STARMAP-002 [P] Tim thay ngoi sao – tra ve Star")
        void getStar_found_returnsStar() {
            Star s = star(1L, 5, 10, true, 500L);
            given(starRepository.findByUserIdAndStarId(1L, 5)).willReturn(Optional.of(s));
            assertThat(starMapService.getStar(1L, 5).getLevel()).isEqualTo(10);
        }

        @Test
        @DisplayName("TC-STARMAP-003 [N] Ngoi sao khong ton tai – nem StarMapServiceException")
        void getStar_notFound_throws() {
            given(starRepository.findByUserIdAndStarId(1L, 99)).willReturn(Optional.empty());
            assertThatThrownBy(() -> starMapService.getStar(1L, 99))
                    .isInstanceOf(StarMapServiceException.class);
        }
    }

    // =========================================================
    // activateStar()
    // =========================================================
    @Nested
    @DisplayName("activateStar()")
    class ActivateStar {

        @Test
        @DisplayName("TC-STARMAP-004 [N] Ngoi sao da duoc kich hoat – nem STAR_ALREADY_ACTIVE")
        void activateStar_alreadyActive_throws() {
            Star s = star(1L, 1, 1, true, 0L);
            given(starRepository.findByUserIdAndStarId(1L, 1)).willReturn(Optional.of(s));
            assertThatThrownBy(() -> starMapService.activateStar(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-005 [P] Kich hoat ngoi sao moi – set active=true, level=1")
        void activateStar_newStar_setsActiveAndLevel() {
            given(starRepository.findByUserIdAndStarId(1L, 2)).willReturn(Optional.empty());
            given(starRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Star result = starMapService.activateStar(1L, 2);

            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getLevel()).isEqualTo(1);
        }
    }

    // =========================================================
    // levelUpStar()
    // =========================================================
    @Nested
    @DisplayName("levelUpStar()")
    class LevelUpStar {

        @Test
        @DisplayName("TC-STARMAP-006 [N] Ngoi sao chua duoc kich hoat – nem STAR_NOT_ACTIVE")
        void levelUpStar_notActive_throws() {
            Star s = star(1L, 1, 5, false, 0L);
            given(starRepository.findByUserIdAndStarId(1L, 1)).willReturn(Optional.of(s));
            assertThatThrownBy(() -> starMapService.levelUpStar(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-007 [N] Ngoi sao dat cap toi da (50) – nem MAX_STAR_LEVEL_REACHED")
        void levelUpStar_maxLevel_throws() {
            Star s = star(1L, 1, 50, true, 0L);
            given(starRepository.findByUserIdAndStarId(1L, 1)).willReturn(Optional.of(s));
            assertThatThrownBy(() -> starMapService.levelUpStar(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-008 [P] Tang cap ngoi sao thanh cong – level++")
        void levelUpStar_success_incrementsLevel() {
            Star s = star(1L, 1, 10, true, 0L);
            given(starRepository.findByUserIdAndStarId(1L, 1)).willReturn(Optional.of(s));
            given(starRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Star result = starMapService.levelUpStar(1L, 1);

            assertThat(result.getLevel()).isEqualTo(11);
        }
    }

    // =========================================================
    // addStarEnergy()
    // =========================================================
    @Nested
    @DisplayName("addStarEnergy()")
    class AddStarEnergy {

        @Test
        @DisplayName("TC-STARMAP-009 [P] Them nang luong ngoi sao – cong don")
        void addStarEnergy_success_addsEnergy() {
            Star s = star(1L, 1, 5, true, 100L);
            given(starRepository.findByUserIdAndStarId(1L, 1)).willReturn(Optional.of(s));
            given(starRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Star result = starMapService.addStarEnergy(1L, 1, 200L);

            assertThat(result.getEnergy()).isEqualTo(300L);
        }
    }

    // =========================================================
    // unlockConstellation()
    // =========================================================
    @Nested
    @DisplayName("unlockConstellation()")
    class UnlockConstellation {

        @Test
        @DisplayName("TC-STARMAP-010 [N] Chom sao da duoc mo – nem CONSTELLATION_ALREADY_UNLOCKED")
        void unlockConstellation_alreadyUnlocked_throws() {
            Constellation c = constellation(1L, 1, 1, true, false, 0, 12, 0L);
            given(constellationRepository.findByUserIdAndConstellationId(1L, 1)).willReturn(Optional.of(c));
            assertThatThrownBy(() -> starMapService.unlockConstellation(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-011 [P] Mo khoa chom sao moi – set unlocked=true")
        void unlockConstellation_new_setsUnlocked() {
            given(constellationRepository.findByUserIdAndConstellationId(1L, 2)).willReturn(Optional.empty());
            given(constellationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Constellation result = starMapService.unlockConstellation(1L, 2);

            assertThat(result.getIsUnlocked()).isTrue();
        }
    }

    // =========================================================
    // levelUpConstellation()
    // =========================================================
    @Nested
    @DisplayName("levelUpConstellation()")
    class LevelUpConstellation {

        @Test
        @DisplayName("TC-STARMAP-012 [N] Chom sao chua mo – nem CONSTELLATION_NOT_UNLOCKED")
        void levelUpConstellation_notUnlocked_throws() {
            Constellation c = constellation(1L, 1, 5, false, false, 0, 12, 0L);
            given(constellationRepository.findByUserIdAndConstellationId(1L, 1)).willReturn(Optional.of(c));
            assertThatThrownBy(() -> starMapService.levelUpConstellation(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-013 [N] Chom sao dat cap toi da (20) – nem MAX_CONSTELLATION_LEVEL")
        void levelUpConstellation_maxLevel_throws() {
            Constellation c = constellation(1L, 1, 20, true, false, 5, 12, 0L);
            given(constellationRepository.findByUserIdAndConstellationId(1L, 1)).willReturn(Optional.of(c));
            assertThatThrownBy(() -> starMapService.levelUpConstellation(1L, 1))
                    .isInstanceOf(StarMapServiceException.class);
        }

        @Test
        @DisplayName("TC-STARMAP-014 [P] Tang cap chom sao thanh cong – level++")
        void levelUpConstellation_success_incrementsLevel() {
            Constellation c = constellation(1L, 1, 5, true, false, 5, 12, 0L);
            given(constellationRepository.findByUserIdAndConstellationId(1L, 1)).willReturn(Optional.of(c));
            given(constellationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Constellation result = starMapService.levelUpConstellation(1L, 1);

            assertThat(result.getLevel()).isEqualTo(6);
        }
    }

    // =========================================================
    // calculateConstellationPower()
    // =========================================================
    @Nested
    @DisplayName("calculateConstellationPower()")
    class CalculateConstellationPower {

        @Test
        @DisplayName("TC-STARMAP-015 [N] Chom sao chua mo – suc manh = 0")
        void calculateConstellationPower_notUnlocked_returnsZero() {
            Constellation c = constellation(1L, 1, 5, false, false, 0, 12, 0L);
            assertThat(starMapService.calculateConstellationPower(c)).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-STARMAP-016 [P] Chom sao hoan thanh – cong them bonus hoan thanh")
        void calculateConstellationPower_completedConstellation_includesBonus() {
            // level=5 → 5*500=2500; completed → +5000; starsActivated=12 → 12*200=2400; power=100
            // Total = 2500 + 5000 + 2400 + 100 = 10000
            Constellation c = constellation(1L, 1, 5, true, true, 12, 12, 100L);
            assertThat(starMapService.calculateConstellationPower(c)).isEqualTo(10000L);
        }

        @Test
        @DisplayName("TC-STARMAP-017 [P] Chom sao chua hoan thanh – khong co bonus hoan thanh")
        void calculateConstellationPower_incompleteConstellation_noCompletionBonus() {
            // level=3 → 1500; not completed → 0; starsActivated=4 → 800; power=0
            // Total = 1500 + 0 + 800 + 0 = 2300
            Constellation c = constellation(1L, 1, 3, true, false, 4, 12, 0L);
            assertThat(starMapService.calculateConstellationPower(c)).isEqualTo(2300L);
        }
    }
}
