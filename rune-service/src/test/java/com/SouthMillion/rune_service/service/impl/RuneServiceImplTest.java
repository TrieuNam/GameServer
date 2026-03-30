package com.SouthMillion.rune_service.service.impl;

import com.SouthMillion.rune_service.client.BagClient;
import com.SouthMillion.rune_service.client.WalletClient;
import com.SouthMillion.rune_service.exception.RuneServiceException;
import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.repository.RuneRepository;
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
@DisplayName("RuneServiceImpl Tests")
class RuneServiceImplTest {

    @Mock private RuneRepository runeRepository;
    @Mock private WalletClient   walletClient;
    @Mock private BagClient      bagClient;

    @InjectMocks private RuneServiceImpl runeService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Rune rune(Long userId, Integer index, int level, int quality,
                             int star, int refinement, boolean equipped) {
        Rune r = new Rune();
        r.setUserId(userId);
        r.setRuneIndex(index);
        r.setLevel(level);
        r.setQuality(quality);
        r.setStar(star);
        r.setRefinementLevel(refinement);
        r.setIsEquipped(equipped);
        r.setEquipSlot(equipped ? 1 : null);
        r.setMainAttrType(1);
        r.setMainAttrValue(100L * quality);
        r.setExp(0L);
        r.setSubAttr1Value(null);
        r.setSubAttr2Value(null);
        r.setSubAttr3Value(null);
        return r;
    }

    // =========================================================
    // getAllRunes()
    // =========================================================
    @Nested
    @DisplayName("getAllRunes()")
    class GetAllRunes {

        @Test
        @DisplayName("TC-RUNE-001 [P] Lay tat ca rune cua nguoi dung")
        void getAllRunes_returnsAll() {
            given(runeRepository.findByUserId(1L)).willReturn(List.of(rune(1L, 1, 10, 2, 1, 0, false)));

            assertThat(runeService.getAllRunes(1L)).hasSize(1);
        }
    }

    // =========================================================
    // getRune()
    // =========================================================
    @Nested
    @DisplayName("getRune()")
    class GetRune {

        @Test
        @DisplayName("TC-RUNE-002 [P] Tim thay rune – tra ve rune")
        void getRune_found_returnsRune() {
            Rune r = rune(1L, 1, 10, 2, 1, 0, false);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));

            assertThat(runeService.getRune(1L, 1)).isEqualTo(r);
        }

        @Test
        @DisplayName("TC-RUNE-003 [N] Khong tim thay rune – nem RUNE_NOT_FOUND")
        void getRune_notFound_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 99)).willReturn(Optional.empty());

            assertThatThrownBy(() -> runeService.getRune(1L, 99))
                    .isInstanceOf(RuneServiceException.class);
        }
    }

    // =========================================================
    // createRune()
    // =========================================================
    @Nested
    @DisplayName("createRune()")
    class CreateRune {

        @Test
        @DisplayName("TC-RUNE-004 [P] Tao rune moi – gan index tiep theo va luu")
        void createRune_assignsNextIndexAndSaves() {
            given(runeRepository.findMaxRuneIndexByUserId(1L)).willReturn(3);
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.createRune(1L, 101, 3);

            assertThat(result.getRuneIndex()).isEqualTo(4); // max + 1
            assertThat(result.getRuneId()).isEqualTo(101);
            assertThat(result.getQuality()).isEqualTo(3);
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getIsEquipped()).isFalse();
        }
    }

    // =========================================================
    // deleteRune()
    // =========================================================
    @Nested
    @DisplayName("deleteRune()")
    class DeleteRune {

        @Test
        @DisplayName("TC-RUNE-005 [N] Rune dang trang bi – nem RUNE_EQUIPPED")
        void deleteRune_equipped_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 2, 1, 0, true)));

            assertThatThrownBy(() -> runeService.deleteRune(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-006 [P] Rune khong trang bi – xoa rune")
        void deleteRune_notEquipped_deletesRune() {
            Rune r = rune(1L, 1, 10, 2, 1, 0, false);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));

            assertThatNoException().isThrownBy(() -> runeService.deleteRune(1L, 1));
            then(runeRepository).should().delete(r);
        }
    }

    // =========================================================
    // levelUpRune()
    // =========================================================
    @Nested
    @DisplayName("levelUpRune()")
    class LevelUpRune {

        @Test
        @DisplayName("TC-RUNE-007 [N] Rune da dat cap toi da (100) – nem MAX_RUNE_LEVEL")
        void levelUpRune_maxLevel_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 100, 2, 1, 0, false)));

            assertThatThrownBy(() -> runeService.levelUpRune(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-008 [P] Tang cap rune thanh cong – level++ va mainAttrValue tang")
        void levelUpRune_success_incrementsLevelAndAttribute() {
            Rune r = rune(1L, 1, 10, 2, 1, 0, false);
            r.setMainAttrValue(200L); // quality=2, mainAttrValue=200
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.levelUpRune(1L, 1);

            assertThat(result.getLevel()).isEqualTo(11);
            assertThat(result.getMainAttrValue()).isEqualTo(220L); // 200 + quality*10 = 200+20
            then(walletClient).should().consumeCurrency(anyString(), any());
        }
    }

    // =========================================================
    // upgradeRuneQuality()
    // =========================================================
    @Nested
    @DisplayName("upgradeRuneQuality()")
    class UpgradeRuneQuality {

        @Test
        @DisplayName("TC-RUNE-009 [N] Rune da dat chat luong toi da (5) – nem MAX_RUNE_QUALITY")
        void upgradeRuneQuality_maxQuality_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 5, 1, 0, false)));

            assertThatThrownBy(() -> runeService.upgradeRuneQuality(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-010 [P] Nang cap chat luong rune thanh cong – quality++")
        void upgradeRuneQuality_success_incrementsQuality() {
            Rune r = rune(1L, 1, 10, 3, 1, 0, false);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.upgradeRuneQuality(1L, 1);

            assertThat(result.getQuality()).isEqualTo(4);
            then(walletClient).should().consumeCurrency(anyString(), any());
        }
    }

    // =========================================================
    // upgradeRuneStar()
    // =========================================================
    @Nested
    @DisplayName("upgradeRuneStar()")
    class UpgradeRuneStar {

        @Test
        @DisplayName("TC-RUNE-011 [N] Rune da dat sao toi da (10) – nem MAX_RUNE_STAR")
        void upgradeRuneStar_maxStar_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 2, 10, 0, false)));

            assertThatThrownBy(() -> runeService.upgradeRuneStar(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-012 [P] Nang cap sao rune thanh cong – star++")
        void upgradeRuneStar_success_incrementsStar() {
            Rune r = rune(1L, 1, 10, 2, 3, 0, false);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.upgradeRuneStar(1L, 1);

            assertThat(result.getStar()).isEqualTo(4);
        }
    }

    // =========================================================
    // refineRune()
    // =========================================================
    @Nested
    @DisplayName("refineRune()")
    class RefineRune {

        @Test
        @DisplayName("TC-RUNE-013 [N] Rune da dat luyen hoa toi da (20) – nem MAX_REFINEMENT_LEVEL")
        void refineRune_maxRefinement_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 2, 1, 20, false)));

            assertThatThrownBy(() -> runeService.refineRune(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-014 [P] Luyen hoa rune thanh cong – refinementLevel++ va tang thuoc tinh")
        void refineRune_success_incrementsRefinementAndBoostsAttrs() {
            Rune r = rune(1L, 1, 10, 2, 1, 5, false);
            r.setMainAttrValue(300L);
            r.setSubAttr1Value(100L);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.refineRune(1L, 1);

            assertThat(result.getRefinementLevel()).isEqualTo(6);
            // boost = quality*5 = 10; mainAttrValue = 300+10=310
            assertThat(result.getMainAttrValue()).isEqualTo(310L);
            assertThat(result.getSubAttr1Value()).isEqualTo(110L); // 100+10
        }
    }

    // =========================================================
    // addRuneExp()
    // =========================================================
    @Nested
    @DisplayName("addRuneExp()")
    class AddRuneExp {

        @Test
        @DisplayName("TC-RUNE-015 [P] Them exp chua du de tang cap – chi cong exp")
        void addRuneExp_notEnoughForLevelUp_addsExp() {
            Rune r = rune(1L, 1, 5, 2, 1, 0, false);
            r.setExp(0L);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.addRuneExp(1L, 1, 500L);

            assertThat(result.getExp()).isEqualTo(500L);
            assertThat(result.getLevel()).isEqualTo(5); // no level up
        }

        @Test
        @DisplayName("TC-RUNE-016 [P] Them exp du de tang cap – tu dong tang cap")
        void addRuneExp_enoughForLevelUp_autoLevels() {
            Rune r = rune(1L, 1, 5, 2, 1, 0, false);
            r.setExp(0L);
            r.setMainAttrValue(100L);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // level=5 needs 5000 exp to level up; give 6000
            Rune result = runeService.addRuneExp(1L, 1, 6000L);

            assertThat(result.getLevel()).isEqualTo(6); // leveled up once
        }
    }

    // =========================================================
    // equipRune()
    // =========================================================
    @Nested
    @DisplayName("equipRune()")
    class EquipRune {

        @Test
        @DisplayName("TC-RUNE-017 [N] Rune da duoc trang bi – nem RUNE_ALREADY_EQUIPPED")
        void equipRune_alreadyEquipped_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 2, 1, 0, true)));

            assertThatThrownBy(() -> runeService.equipRune(1L, 1, 2))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-018 [P] Trang bi rune vao slot – set isEquipped=true")
        void equipRune_success_setsEquipped() {
            Rune r = rune(1L, 1, 10, 2, 1, 0, false);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Rune result = runeService.equipRune(1L, 1, 3);

            assertThat(result.getIsEquipped()).isTrue();
            assertThat(result.getEquipSlot()).isEqualTo(3);
            then(runeRepository).should().unequipRuneFromSlot(1L, 3);
        }
    }

    // =========================================================
    // unequipRune()
    // =========================================================
    @Nested
    @DisplayName("unequipRune()")
    class UnequipRune {

        @Test
        @DisplayName("TC-RUNE-019 [N] Rune khong duoc trang bi – nem RUNE_NOT_EQUIPPED")
        void unequipRune_notEquipped_throws() {
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1))
                    .willReturn(Optional.of(rune(1L, 1, 10, 2, 1, 0, false)));

            assertThatThrownBy(() -> runeService.unequipRune(1L, 1))
                    .isInstanceOf(RuneServiceException.class);
        }

        @Test
        @DisplayName("TC-RUNE-020 [P] Thao trang bi rune – set isEquipped=false")
        void unequipRune_equipped_setsUnequipped() {
            Rune r = rune(1L, 1, 10, 2, 1, 0, true);
            given(runeRepository.findByUserIdAndRuneIndex(1L, 1)).willReturn(Optional.of(r));
            given(runeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() -> runeService.unequipRune(1L, 1));
            assertThat(r.getIsEquipped()).isFalse();
            assertThat(r.getEquipSlot()).isNull();
        }
    }

    // =========================================================
    // calculateRunePower()
    // =========================================================
    @Nested
    @DisplayName("calculateRunePower()")
    class CalculateRunePower {

        @Test
        @DisplayName("TC-RUNE-021 [P] Tinh suc manh rune – tinh chinh xac theo cong thuc")
        void calculateRunePower_correctFormula() {
            // mainAttrValue=200, sub attrs null, level=10, quality=2, star=3, refinement=4
            // power = 200 + 0 + 10*50 + 2*200 + 3*100 + 4*150
            //       = 200 + 500 + 400 + 300 + 600 = 2000
            Rune r = rune(1L, 1, 10, 2, 3, 4, false);
            r.setMainAttrValue(200L);

            assertThat(runeService.calculateRunePower(r)).isEqualTo(2000L);
        }

        @Test
        @DisplayName("TC-RUNE-022 [P] Tinh suc manh rune co sub attrs – cong vao tong")
        void calculateRunePower_withSubAttrs_includesAll() {
            // mainAttrValue=200, sub1=50, sub2=30, sub3=null, level=5, quality=1, star=1, refinement=0
            // power = 200 + 50 + 30 + 5*50 + 1*200 + 1*100 + 0 = 200+80+250+200+100 = 830
            Rune r = rune(1L, 1, 5, 1, 1, 0, false);
            r.setMainAttrValue(200L);
            r.setSubAttr1Value(50L);
            r.setSubAttr2Value(30L);
            r.setSubAttr3Value(null);

            assertThat(runeService.calculateRunePower(r)).isEqualTo(830L);
        }
    }

    // =========================================================
    // calculateTotalRunePower()
    // =========================================================
    @Nested
    @DisplayName("calculateTotalRunePower()")
    class CalculateTotalRunePower {

        @Test
        @DisplayName("TC-RUNE-023 [P] Tinh tong suc manh rune – cong tat ca rune dang trang bi")
        void calculateTotalRunePower_sumsEquippedRunes() {
            // Rune 1: mainAttr=100, level=1, quality=1, star=1, refinement=0
            // power = 100 + 50 + 200 + 100 + 0 = 450
            Rune r1 = rune(1L, 1, 1, 1, 1, 0, true);
            r1.setMainAttrValue(100L);

            // Rune 2: mainAttr=200, level=5, quality=2, star=2, refinement=1
            // power = 200 + 250 + 400 + 200 + 150 = 1200
            Rune r2 = rune(1L, 2, 5, 2, 2, 1, true);
            r2.setMainAttrValue(200L);

            given(runeRepository.findByUserIdAndIsEquippedTrue(1L)).willReturn(List.of(r1, r2));

            assertThat(runeService.calculateTotalRunePower(1L)).isEqualTo(450L + 1200L);
        }

        @Test
        @DisplayName("TC-RUNE-024 [P] Khong co rune trang bi – tong suc manh = 0")
        void calculateTotalRunePower_noEquippedRunes_returnsZero() {
            given(runeRepository.findByUserIdAndIsEquippedTrue(1L)).willReturn(List.of());

            assertThat(runeService.calculateTotalRunePower(1L)).isEqualTo(0L);
        }
    }
}
