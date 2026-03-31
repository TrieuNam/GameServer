package com.SouthMillion.angel_service.service.impl;

import com.SouthMillion.angel_service.client.BagClient;
import com.SouthMillion.angel_service.client.WalletClient;
import com.SouthMillion.angel_service.config.AngelConfigHolder;
import com.SouthMillion.angel_service.event.AngelEventPublisher;
import com.SouthMillion.angel_service.exception.AngelServiceException;
import com.SouthMillion.angel_service.model.entity.Angel;
import com.SouthMillion.angel_service.repository.AngelRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AngelServiceImpl Tests")
class AngelServiceImplTest {

    @Mock private AngelRepository angelRepository;
    @Mock private AngelConfigHolder configHolder;
    @Mock private BagClient bagClient;
    @Mock private WalletClient walletClient;
    @Mock private AngelEventPublisher eventPublisher;

    @InjectMocks private AngelServiceImpl angelService;

    private static final Long USER_ID    = 1001L;
    private static final int  ANGEL_INDEX = 1;
    private static final int  ANGEL_ID    = 1;

    /** Build a fully-populated Angel for test use. */
    private Angel buildAngel(int level, int grade, boolean active) {
        Angel a = new Angel();
        a.setUserId(USER_ID);
        a.setAngelIndex(ANGEL_INDEX);
        a.setAngelId(ANGEL_ID);
        a.setLevel(level);
        a.setGrade(grade);
        a.setExp(0L);
        a.setIsActive(active);
        a.setIsEquipped(false);
        a.setStarLevel(0);
        a.setSkill1Id(101); a.setSkill1Level(0);
        a.setSkill2Id(102); a.setSkill2Level(0);
        a.setSkill3Id(103); a.setSkill3Level(0);
        a.setSkill4Id(104); a.setSkill4Level(0);
        a.setAppearanceId(0);
        a.setEvolutionStage(0);
        a.setBlessingPoints(0L);
        a.setName("TestAngel");
        return a;
    }

    // =========================================================
    // getAllAngels()
    // =========================================================
    @Nested
    @DisplayName("getAllAngels()")
    class GetAllAngels {

        @Test
        @DisplayName("TC-ANGEL-001 [P] Lay danh sach angel – tra ve list day du")
        void getAllAngels_returnsListFromRepository() {
            Angel a1 = buildAngel(1, 1, true);
            Angel a2 = buildAngel(5, 2, false);
            given(angelRepository.findByUserId(USER_ID)).willReturn(List.of(a1, a2));

            List<Angel> result = angelService.getAllAngels(USER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC-ANGEL-002 [P] Khong co angel – tra ve danh sach rong")
        void getAllAngels_noAngels_returnsEmpty() {
            given(angelRepository.findByUserId(USER_ID)).willReturn(List.of());

            List<Angel> result = angelService.getAllAngels(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getAngel()
    // =========================================================
    @Nested
    @DisplayName("getAngel()")
    class GetAngel {

        @Test
        @DisplayName("TC-ANGEL-003 [P] Lay angel ton tai – tra ve angel dung")
        void getAngel_found_returnsAngel() {
            Angel angel = buildAngel(10, 2, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            Angel result = angelService.getAngel(USER_ID, ANGEL_INDEX);

            assertThat(result.getLevel()).isEqualTo(10);
            assertThat(result.getGrade()).isEqualTo(2);
        }

        @Test
        @DisplayName("TC-ANGEL-004 [N] Angel khong ton tai – nem ANGEL_NOT_FOUND")
        void getAngel_notFound_throws() {
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> angelService.getAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("ANGEL_NOT_FOUND");
        }
    }

    // =========================================================
    // unlockAngel()
    // =========================================================
    @Nested
    @DisplayName("unlockAngel()")
    class UnlockAngel {

        @Test
        @DisplayName("TC-ANGEL-005 [P] Mo khoa angel moi – tieu ton vang + vat lieu roi luu")
        void unlockAngel_newAngel_consumesResourcesAndSaves() {
            given(angelRepository.findByUserIdAndAngelId(USER_ID, ANGEL_ID))
                    .willReturn(Optional.empty());
            given(configHolder.getUnlockCost(ANGEL_ID))
                    .willReturn(new AngelConfigHolder.UnlockCost(5000L, 2001, 10));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.unlockAngel(USER_ID, ANGEL_ID);

            assertThat(result).isNotNull();
            then(walletClient).should().consumeCurrency(anyString(), any());
            then(bagClient).should().useItem(anyString(), any());
        }

        @Test
        @DisplayName("TC-ANGEL-006 [N] Angel da ton tai – nem ANGEL_ALREADY_EXISTS")
        void unlockAngel_alreadyExists_throws() {
            Angel existing = buildAngel(1, 1, false);
            given(angelRepository.findByUserIdAndAngelId(USER_ID, ANGEL_ID))
                    .willReturn(Optional.of(existing));

            assertThatThrownBy(() -> angelService.unlockAngel(USER_ID, ANGEL_ID))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("ANGEL_ALREADY_EXISTS");
        }
    }

    // =========================================================
    // levelUpAngel()
    // =========================================================
    @Nested
    @DisplayName("levelUpAngel()")
    class LevelUpAngel {

        @Test
        @DisplayName("TC-ANGEL-007 [P] Tang cap angel hop le – level tang 1, exp reset = 0")
        void levelUpAngel_valid_incrementsLevelAndResetsExp() {
            Angel angel = buildAngel(50, 5, true);
            angel.setExp(999L);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getLevelUpCost(50))
                    .willReturn(new AngelConfigHolder.LevelUpCost(7500L, 2002, 150));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.levelUpAngel(USER_ID, ANGEL_INDEX);

            assertThat(result.getLevel()).isEqualTo(51);
            assertThat(result.getExp()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-ANGEL-008 [N] Angel khong active – nem ANGEL_NOT_ACTIVE")
        void levelUpAngel_notActive_throws() {
            Angel angel = buildAngel(50, 5, false);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.levelUpAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("ANGEL_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-ANGEL-009 [N] Level toi da (100) – nem MAX_LEVEL_REACHED")
        void levelUpAngel_atMaxLevel_throws() {
            Angel angel = buildAngel(100, 5, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.levelUpAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("MAX_LEVEL_REACHED");
        }
    }

    // =========================================================
    // gradeUpAngel()
    // =========================================================
    @Nested
    @DisplayName("gradeUpAngel()")
    class GradeUpAngel {

        @Test
        @DisplayName("TC-ANGEL-010 [P] Tang phom hop le – grade tang 1, level reset = 1, exp = 0")
        void gradeUpAngel_valid_incrementsGradeAndResetsLevel() {
            Angel angel = buildAngel(100, 5, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getGradeUpCost(5))
                    .willReturn(new AngelConfigHolder.GradeUpCost(10000L, 2003, 40));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.gradeUpAngel(USER_ID, ANGEL_INDEX);

            assertThat(result.getGrade()).isEqualTo(6);
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getExp()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-ANGEL-011 [N] Phom toi da (10) – nem MAX_GRADE_REACHED")
        void gradeUpAngel_atMaxGrade_throws() {
            Angel angel = buildAngel(100, 10, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.gradeUpAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("MAX_GRADE_REACHED");
        }
    }

    // =========================================================
    // equipAngel() / unequipAngel()
    // =========================================================
    @Nested
    @DisplayName("equipAngel() / unequipAngel()")
    class EquipUnequip {

        @Test
        @DisplayName("TC-ANGEL-012 [P] Trang bi angel active – bo trang bi cu roi trang bi moi")
        void equipAngel_active_unequipsAllThenSetsEquipped() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            ArgumentCaptor<Angel> captor = ArgumentCaptor.forClass(Angel.class);
            given(angelRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            angelService.equipAngel(USER_ID, ANGEL_INDEX);

            then(angelRepository).should().unequipAllAngels(USER_ID);
            assertThat(captor.getValue().getIsEquipped()).isTrue();
        }

        @Test
        @DisplayName("TC-ANGEL-013 [N] Trang bi angel khong active – nem ANGEL_NOT_ACTIVE")
        void equipAngel_notActive_throws() {
            Angel angel = buildAngel(1, 1, false);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.equipAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("ANGEL_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-ANGEL-014 [P] Go trang bi – goi unequipAllAngels cho userId")
        void unequipAngel_callsUnequipAll() {
            angelService.unequipAngel(USER_ID);

            then(angelRepository).should().unequipAllAngels(USER_ID);
        }
    }

    // =========================================================
    // upgradeStarLevel()
    // =========================================================
    @Nested
    @DisplayName("upgradeStarLevel()")
    class UpgradeStarLevel {

        @Test
        @DisplayName("TC-ANGEL-015 [P] Nang sao hop le – starLevel tang 1")
        void upgradeStarLevel_valid_incrementsStar() {
            Angel angel = buildAngel(1, 1, true);
            angel.setStarLevel(5);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getStarUpCost(5))
                    .willReturn(new AngelConfigHolder.StarUpCost(4800L, 2004, 30));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.upgradeStarLevel(USER_ID, ANGEL_INDEX);

            assertThat(result.getStarLevel()).isEqualTo(6);
        }

        @Test
        @DisplayName("TC-ANGEL-016 [N] Sao toi da (12) – nem MAX_STAR_REACHED")
        void upgradeStarLevel_atMaxStar_throws() {
            Angel angel = buildAngel(1, 1, true);
            angel.setStarLevel(12);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.upgradeStarLevel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("MAX_STAR_REACHED");
        }
    }

    // =========================================================
    // upgradeSkill()
    // =========================================================
    @Nested
    @DisplayName("upgradeSkill()")
    class UpgradeSkill {

        @Test
        @DisplayName("TC-ANGEL-017 [P] Nang skill slot 1 – skill1Level tang 1")
        void upgradeSkill_slot1_incrementsSkill1Level() {
            Angel angel = buildAngel(1, 1, true);
            angel.setSkill1Level(3);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getSkillUpCost(3))
                    .willReturn(new AngelConfigHolder.SkillUpCost(2000L, 2005, 8));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.upgradeSkill(USER_ID, ANGEL_INDEX, 1);

            assertThat(result.getSkill1Level()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-ANGEL-018 [P] Nang skill slot 4 – skill4Level tang 1")
        void upgradeSkill_slot4_incrementsSkill4Level() {
            Angel angel = buildAngel(1, 1, true);
            angel.setSkill4Level(0);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getSkillUpCost(0))
                    .willReturn(new AngelConfigHolder.SkillUpCost(500L, 2005, 2));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.upgradeSkill(USER_ID, ANGEL_INDEX, 4);

            assertThat(result.getSkill4Level()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-ANGEL-019 [N] Slot < 1 – nem INVALID_SKILL_SLOT")
        void upgradeSkill_slotBelowOne_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.upgradeSkill(USER_ID, ANGEL_INDEX, 0))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("INVALID_SKILL_SLOT");
        }

        @Test
        @DisplayName("TC-ANGEL-020 [N] Slot > 4 – nem INVALID_SKILL_SLOT")
        void upgradeSkill_slotAboveFour_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.upgradeSkill(USER_ID, ANGEL_INDEX, 5))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("INVALID_SKILL_SLOT");
        }

        @Test
        @DisplayName("TC-ANGEL-021 [N] Skill da max level (10) – nem MAX_SKILL_LEVEL_REACHED")
        void upgradeSkill_atMaxLevel_throws() {
            Angel angel = buildAngel(1, 1, true);
            angel.setSkill2Level(10);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.upgradeSkill(USER_ID, ANGEL_INDEX, 2))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("MAX_SKILL_LEVEL_REACHED");
        }
    }

    // =========================================================
    // setAppearance()
    // =========================================================
    @Nested
    @DisplayName("setAppearance()")
    class SetAppearance {

        @Test
        @DisplayName("TC-ANGEL-022 [P] Doi ngoai hinh hop le – appearanceId duoc luu")
        void setAppearance_valid_updatesAppearanceId() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            ArgumentCaptor<Angel> captor = ArgumentCaptor.forClass(Angel.class);
            given(angelRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            angelService.setAppearance(USER_ID, ANGEL_INDEX, 5);

            assertThat(captor.getValue().getAppearanceId()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-ANGEL-023 [N] AppearanceId am – nem INVALID_APPEARANCE")
        void setAppearance_negativeId_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.setAppearance(USER_ID, ANGEL_INDEX, -1))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("INVALID_APPEARANCE");
        }
    }

    // =========================================================
    // evolveAngel()
    // =========================================================
    @Nested
    @DisplayName("evolveAngel()")
    class EvolveAngel {

        @Test
        @DisplayName("TC-ANGEL-024 [P] Tien hoa hop le – evolutionStage tang 1")
        void evolveAngel_valid_incrementsEvolutionStage() {
            Angel angel = buildAngel(1, 1, true);
            angel.setEvolutionStage(2);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(configHolder.getEvolutionCost(2))
                    .willReturn(new AngelConfigHolder.EvolutionCost(15000L, 2006, 30));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.evolveAngel(USER_ID, ANGEL_INDEX);

            assertThat(result.getEvolutionStage()).isEqualTo(3);
        }

        @Test
        @DisplayName("TC-ANGEL-025 [N] Tien hoa toi da (stage 5) – nem MAX_EVOLUTION_REACHED")
        void evolveAngel_atMaxStage_throws() {
            Angel angel = buildAngel(1, 1, true);
            angel.setEvolutionStage(5);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.evolveAngel(USER_ID, ANGEL_INDEX))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("MAX_EVOLUTION_REACHED");
        }
    }

    // =========================================================
    // addBlessing()
    // =========================================================
    @Nested
    @DisplayName("addBlessing()")
    class AddBlessing {

        @Test
        @DisplayName("TC-ANGEL-026 [P] Them blessing – diem cong don tich luy")
        void addBlessing_addsPointsCumulatively() {
            Angel angel = buildAngel(1, 1, true);
            angel.setBlessingPoints(100L);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.addBlessing(USER_ID, ANGEL_INDEX, 50L);

            assertThat(result.getBlessingPoints()).isEqualTo(150L);
        }
    }

    // =========================================================
    // calculateAngelPower()
    // =========================================================
    @Nested
    @DisplayName("calculateAngelPower()")
    class CalculateAngelPower {

        @Test
        @DisplayName("TC-ANGEL-027 [P] Angel khong active – chieu chien = 0")
        void calculateAngelPower_inactive_returnsZero() {
            Angel angel = buildAngel(50, 5, false);

            Long power = angelService.calculateAngelPower(angel);

            assertThat(power).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-ANGEL-028 [P] Angel moi unlock (level 1, grade 1) – chieu chien = 950")
        void calculateAngelPower_newAngel_returnsBasePower() {
            Angel angel = buildAngel(1, 1, true);
            // level*150=150, grade*800=800, star=0, evo=0, skills=0, bless=0 → 950

            Long power = angelService.calculateAngelPower(angel);

            assertThat(power).isEqualTo(950L);
        }

        @Test
        @DisplayName("TC-ANGEL-029 [P] Angel level 10 grade 2, star 3, evo 1, skills 3, bless 100 – cong thuc chuan xac")
        void calculateAngelPower_active_calculatesCorrectly() {
            Angel angel = buildAngel(10, 2, true);
            angel.setStarLevel(3);
            angel.setEvolutionStage(1);
            angel.setSkill1Level(2);
            angel.setSkill2Level(1);
            angel.setSkill3Level(0);
            angel.setSkill4Level(0);
            angel.setBlessingPoints(100L);
            // level*150=1500, grade*800=1600, star*300=900, evo*1000=1000
            // skills=(2+1+0+0)*100=300, bless=100/10=10
            // total = 1500+1600+900+1000+300+10 = 5310

            Long power = angelService.calculateAngelPower(angel);

            assertThat(power).isEqualTo(5310L);
        }
    }

    // =========================================================
    // canLevelUp() / canGradeUp() / canEvolve()
    // =========================================================
    @Nested
    @DisplayName("canLevelUp() / canGradeUp() / canEvolve()")
    class CanUpgradeMethods {

        @Test
        @DisplayName("TC-ANGEL-030 [P] Level 50 < 100 – co the tang cap")
        void canLevelUp_belowMax_returnsTrue() {
            Angel angel = buildAngel(50, 5, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canLevelUp(USER_ID, ANGEL_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ANGEL-031 [N] Level 100 = max – khong the tang cap")
        void canLevelUp_atMax_returnsFalse() {
            Angel angel = buildAngel(100, 5, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canLevelUp(USER_ID, ANGEL_INDEX)).isFalse();
        }

        @Test
        @DisplayName("TC-ANGEL-032 [P] Grade 9 < 10 – co the tang phom")
        void canGradeUp_belowMax_returnsTrue() {
            Angel angel = buildAngel(100, 9, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canGradeUp(USER_ID, ANGEL_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ANGEL-033 [N] Grade 10 = max – khong the tang phom")
        void canGradeUp_atMax_returnsFalse() {
            Angel angel = buildAngel(100, 10, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canGradeUp(USER_ID, ANGEL_INDEX)).isFalse();
        }

        @Test
        @DisplayName("TC-ANGEL-034 [P] EvolutionStage 4 < 5 – co the tien hoa")
        void canEvolve_belowMax_returnsTrue() {
            Angel angel = buildAngel(1, 1, true);
            angel.setEvolutionStage(4);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canEvolve(USER_ID, ANGEL_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ANGEL-035 [N] EvolutionStage 5 = max – khong the tien hoa")
        void canEvolve_atMax_returnsFalse() {
            Angel angel = buildAngel(1, 1, true);
            angel.setEvolutionStage(5);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThat(angelService.canEvolve(USER_ID, ANGEL_INDEX)).isFalse();
        }
    }

    // =========================================================
    // renameAngel()
    // =========================================================
    @Nested
    @DisplayName("renameAngel()")
    class RenameAngel {

        @Test
        @DisplayName("TC-ANGEL-036 [P] Doi ten hop le – ten moi duoc luu, tieu ton 10000 vang")
        void renameAngel_validName_updatesNameAndConsumesGold() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));
            given(angelRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Angel result = angelService.renameAngel(USER_ID, ANGEL_INDEX, "NewName");

            assertThat(result.getName()).isEqualTo("NewName");
            then(walletClient).should().consumeCurrency(anyString(), any());
        }

        @Test
        @DisplayName("TC-ANGEL-037 [N] Ten null – nem INVALID_NAME")
        void renameAngel_nullName_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.renameAngel(USER_ID, ANGEL_INDEX, null))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("INVALID_NAME");
        }

        @Test
        @DisplayName("TC-ANGEL-038 [N] Ten rong – nem INVALID_NAME")
        void renameAngel_emptyName_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.renameAngel(USER_ID, ANGEL_INDEX, ""))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("INVALID_NAME");
        }

        @Test
        @DisplayName("TC-ANGEL-039 [N] Ten > 12 ky tu – nem NAME_TOO_LONG")
        void renameAngel_tooLongName_throws() {
            Angel angel = buildAngel(1, 1, true);
            given(angelRepository.findByUserIdAndAngelIndex(USER_ID, ANGEL_INDEX))
                    .willReturn(Optional.of(angel));

            assertThatThrownBy(() -> angelService.renameAngel(USER_ID, ANGEL_INDEX, "VeryLongNameXXX"))
                    .isInstanceOf(AngelServiceException.class)
                    .hasMessageContaining("NAME_TOO_LONG");
        }
    }
}
