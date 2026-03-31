package com.SouthMillion.mount_service.service.impl;

import com.SouthMillion.mount_service.client.BagClient;
import com.SouthMillion.mount_service.client.WalletClient;
import com.SouthMillion.mount_service.config.MountConfigHolder;
import com.SouthMillion.mount_service.exception.MountServiceException;
import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.publisher.MountEventPublisher;
import com.SouthMillion.mount_service.repository.MountRepository;
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
@DisplayName("MountServiceImpl Tests")
class MountServiceImplTest {

    @Mock private MountRepository        mountRepository;
    @Mock private MountConfigHolder      configHolder;
    @Mock private BagClient              bagClient;
    @Mock private WalletClient           walletClient;
    @Mock private MountEventPublisher    mountEventPublisher;

    @Mock private MountConfigHolder.LevelUpCost levelUpCost;
    @Mock private MountConfigHolder.GradeUpCost gradeUpCost;
    @Mock private MountConfigHolder.StarUpCost  starUpCost;

    @InjectMocks private MountServiceImpl mountService;

    private static Mount mount(Long userId, int idx, int level, int grade,
                                int star, int skin, boolean active, boolean equipped) {
        Mount m = new Mount();
        m.setUserId(userId);
        m.setMountIndex(idx);
        m.setMountId(10);
        m.setLevel(level);
        m.setGrade(grade);
        m.setStarLevel(star);
        m.setSkinLevel(skin);
        m.setIsActive(active);
        m.setIsEquipped(equipped);
        m.setExp(0L);
        m.setExploreProgress(0L);
        return m;
    }

    // =========================================================
    // getAllMounts
    // =========================================================
    @Nested
    @DisplayName("getAllMounts()")
    class GetAllMounts {

        @Test
        @DisplayName("TC-MNT-001 [P] Lay tat ca mount cua nguoi dung")
        void getAllMounts_returnsList() {
            Mount m = mount(1L, 0, 1, 1, 0, 0, true, false);
            given(mountRepository.findByUserId(1L)).willReturn(List.of(m));

            List<Mount> result = mountService.getAllMounts(1L);

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================
    // getMount
    // =========================================================
    @Nested
    @DisplayName("getMount()")
    class GetMount {

        @Test
        @DisplayName("TC-MNT-002 [P] Lay mount ton tai – tra ve mount")
        void getMount_found_returnsMount() {
            Mount m = mount(1L, 0, 5, 2, 1, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            Mount result = mountService.getMount(1L, 0);

            assertThat(result.getLevel()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-MNT-003 [N] Mount khong ton tai – nem MountServiceException")
        void getMount_notFound_throwsException() {
            given(mountRepository.findByUserIdAndMountIndex(1L, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mountService.getMount(1L, 99))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MOUNT_NOT_FOUND");
        }
    }

    // =========================================================
    // unlockMount
    // =========================================================
    @Nested
    @DisplayName("unlockMount()")
    class UnlockMount {

        @Test
        @DisplayName("TC-MNT-004 [P] Mo khoa mount thanh cong")
        void unlockMount_success_savesMount() {
            given(configHolder.getUnlockCost(10)).willReturn(5000L);
            given(mountRepository.findByUserId(1L)).willReturn(List.of());
            given(mountRepository.save(any(Mount.class))).willAnswer(inv -> inv.getArgument(0));

            Mount result = mountService.unlockMount(1L, 10);

            assertThat(result.getMountId()).isEqualTo(10);
            assertThat(result.getLevel()).isEqualTo(1);
            then(walletClient).should().consumeCurrency(anyString(), any());
            then(mountEventPublisher).should().publishMountUnlocked(1L, 10, 0);
        }

        @Test
        @DisplayName("TC-MNT-005 [N] Wallet loi – nem MountServiceException WALLET_ERROR")
        void unlockMount_walletError_throwsException() {
            given(configHolder.getUnlockCost(10)).willReturn(5000L);
            given(walletClient.consumeCurrency(anyString(), any()))
                    .willThrow(new RuntimeException("Network error"));

            assertThatThrownBy(() -> mountService.unlockMount(1L, 10))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("WALLET_ERROR");
        }
    }

    // =========================================================
    // levelUpMount
    // =========================================================
    @Nested
    @DisplayName("levelUpMount()")
    class LevelUpMount {

        @Test
        @DisplayName("TC-MNT-006 [P] Tang cap mount thanh cong")
        void levelUpMount_success_incrementsLevel() {
            Mount m = mount(1L, 0, 5, 1, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));
            given(configHolder.getLevelUpCost(5)).willReturn(levelUpCost);
            given(levelUpCost.getGoldCost()).willReturn(1000L);
            given(levelUpCost.getMaterialItemId()).willReturn(2);
            given(levelUpCost.getMaterialQuantity()).willReturn(3);
            given(mountRepository.save(any(Mount.class))).willAnswer(inv -> inv.getArgument(0));

            Mount result = mountService.levelUpMount(1L, 0);

            assertThat(result.getLevel()).isEqualTo(6);
        }

        @Test
        @DisplayName("TC-MNT-007 [N] Mount chua duoc kich hoat – nem MOUNT_NOT_ACTIVE")
        void levelUpMount_notActive_throwsException() {
            Mount m = mount(1L, 0, 5, 1, 0, 0, false, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> mountService.levelUpMount(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MOUNT_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-MNT-008 [N] Mount da dat cap toi da – nem MAX_LEVEL_REACHED")
        void levelUpMount_maxLevel_throwsException() {
            Mount m = mount(1L, 0, 100, 1, 0, 0, true, false); // MAX_MOUNT_LEVEL = 100
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> mountService.levelUpMount(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MAX_LEVEL_REACHED");
        }

        @Test
        @DisplayName("TC-MNT-009 [N] Bag loi khi tieu thu nguyen lieu – nem MOUNT_BAG_ERROR")
        void levelUpMount_bagError_throwsException() {
            Mount m = mount(1L, 0, 5, 1, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));
            given(configHolder.getLevelUpCost(5)).willReturn(levelUpCost);
            given(levelUpCost.getGoldCost()).willReturn(0L); // skip gold
            given(levelUpCost.getMaterialItemId()).willReturn(2);
            given(levelUpCost.getMaterialQuantity()).willReturn(5);
            willThrow(new RuntimeException("Bag error")).given(bagClient).useItem(anyString(), any());

            assertThatThrownBy(() -> mountService.levelUpMount(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("BAG_ERROR");
        }
    }

    // =========================================================
    // gradeUpMount
    // =========================================================
    @Nested
    @DisplayName("gradeUpMount()")
    class GradeUpMount {

        @Test
        @DisplayName("TC-MNT-010 [P] Nang cap hang mount thanh cong")
        void gradeUpMount_success_incrementsGrade() {
            Mount m = mount(1L, 0, 10, 3, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));
            given(configHolder.getGradeUpCost(3)).willReturn(gradeUpCost);
            given(gradeUpCost.getGoldCost()).willReturn(2000L);
            given(gradeUpCost.getMaterialItemId()).willReturn(3);
            given(gradeUpCost.getMaterialQuantity()).willReturn(5);
            given(mountRepository.save(any(Mount.class))).willAnswer(inv -> inv.getArgument(0));

            Mount result = mountService.gradeUpMount(1L, 0);

            assertThat(result.getGrade()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-MNT-011 [N] Mount da dat hang toi da – nem MAX_GRADE_REACHED")
        void gradeUpMount_maxGrade_throwsException() {
            Mount m = mount(1L, 0, 100, 10, 0, 0, true, false); // MAX_MOUNT_GRADE = 10
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> mountService.gradeUpMount(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MAX_GRADE_REACHED");
        }
    }

    // =========================================================
    // equipMount / unequipMount
    // =========================================================
    @Nested
    @DisplayName("equipMount() / unequipMount()")
    class EquipUnequip {

        @Test
        @DisplayName("TC-MNT-012 [P] Trang bi mount thanh cong")
        void equipMount_active_setsEquipped() {
            Mount m = mount(1L, 0, 5, 1, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));
            given(mountRepository.save(any(Mount.class))).willAnswer(inv -> inv.getArgument(0));

            mountService.equipMount(1L, 0);

            then(mountRepository).should().unequipAllMounts(1L);
            assertThat(m.getIsEquipped()).isTrue();
        }

        @Test
        @DisplayName("TC-MNT-013 [N] Mount chua kich hoat – khong the trang bi")
        void equipMount_notActive_throwsException() {
            Mount m = mount(1L, 0, 5, 1, 0, 0, false, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> mountService.equipMount(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MOUNT_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-MNT-014 [P] Bo trang bi mount")
        void unequipMount_callsUnequipAll() {
            mountService.unequipMount(1L);

            then(mountRepository).should().unequipAllMounts(1L);
        }
    }

    // =========================================================
    // upgradeStarLevel
    // =========================================================
    @Nested
    @DisplayName("upgradeStarLevel()")
    class UpgradeStarLevel {

        @Test
        @DisplayName("TC-MNT-015 [P] Tang cap star mount thanh cong")
        void upgradeStarLevel_success_incrementsStar() {
            Mount m = mount(1L, 0, 5, 1, 3, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));
            given(configHolder.getStarUpCost(3)).willReturn(starUpCost);
            given(starUpCost.getGoldCost()).willReturn(3000L);
            given(starUpCost.getMaterialItemId()).willReturn(4);
            given(starUpCost.getMaterialQuantity()).willReturn(10);
            given(mountRepository.save(any(Mount.class))).willAnswer(inv -> inv.getArgument(0));

            Mount result = mountService.upgradeStarLevel(1L, 0);

            assertThat(result.getStarLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-MNT-016 [N] Star da dat toi da – nem MAX_STAR_REACHED")
        void upgradeStarLevel_maxStar_throwsException() {
            Mount m = mount(1L, 0, 50, 5, 10, 0, true, false); // MAX_STAR_LEVEL = 10
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThatThrownBy(() -> mountService.upgradeStarLevel(1L, 0))
                    .isInstanceOf(MountServiceException.class)
                    .hasMessageContaining("MAX_STAR_REACHED");
        }
    }

    // =========================================================
    // calculateMountPower
    // =========================================================
    @Nested
    @DisplayName("calculateMountPower()")
    class CalculateMountPower {

        @Test
        @DisplayName("TC-MNT-017 [P] Mount dang hoat dong – tinh suc manh theo cong thuc")
        void calculateMountPower_active_returnsFormulaPower() {
            // level=10, grade=2, star=1, skin=0
            // power = 10*100 + 2*500 + 1*200 + 0*50 = 1000+1000+200+0 = 2200
            Mount m = mount(1L, 0, 10, 2, 1, 0, true, false);

            Long power = mountService.calculateMountPower(m);

            assertThat(power).isEqualTo(2200L);
        }

        @Test
        @DisplayName("TC-MNT-018 [N] Mount khong hoat dong – suc manh bang 0")
        void calculateMountPower_inactive_returnsZero() {
            Mount m = mount(1L, 0, 50, 5, 5, 2, false, false);

            Long power = mountService.calculateMountPower(m);

            assertThat(power).isEqualTo(0L);
        }
    }

    // =========================================================
    // canLevelUp / canGradeUp
    // =========================================================
    @Nested
    @DisplayName("canLevelUp() / canGradeUp()")
    class CanUpgrade {

        @Test
        @DisplayName("TC-MNT-019 [P] Mount dang hoat dong, chua dat cap toi da – co the tang cap")
        void canLevelUp_activeAndBelowMax_returnsTrue() {
            Mount m = mount(1L, 0, 50, 1, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThat(mountService.canLevelUp(1L, 0)).isTrue();
        }

        @Test
        @DisplayName("TC-MNT-020 [N] Mount da dat cap toi da – khong the tang cap")
        void canLevelUp_maxLevel_returnsFalse() {
            Mount m = mount(1L, 0, 100, 1, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThat(mountService.canLevelUp(1L, 0)).isFalse();
        }

        @Test
        @DisplayName("TC-MNT-021 [P] Mount dang hoat dong, chua dat hang toi da – co the nang hang")
        void canGradeUp_activeAndBelowMax_returnsTrue() {
            Mount m = mount(1L, 0, 50, 5, 0, 0, true, false);
            given(mountRepository.findByUserIdAndMountIndex(1L, 0)).willReturn(Optional.of(m));

            assertThat(mountService.canGradeUp(1L, 0)).isTrue();
        }

        @Test
        @DisplayName("TC-MNT-022 [N] Mount khong ton tai – canLevelUp tra ve false")
        void canLevelUp_mountNotFound_returnsFalse() {
            given(mountRepository.findByUserIdAndMountIndex(1L, 99)).willReturn(Optional.empty());

            assertThat(mountService.canLevelUp(1L, 99)).isFalse();
        }
    }
}
