package com.SouthMillion.artifact_service.service.impl;

import com.SouthMillion.artifact_service.client.BagClient;
import com.SouthMillion.artifact_service.client.RoleClient;
import com.SouthMillion.artifact_service.client.WalletClient;
import com.SouthMillion.artifact_service.exception.ArtifactServiceException;
import com.SouthMillion.artifact_service.model.entity.Artifact;
import com.SouthMillion.artifact_service.repository.ArtifactRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactServiceImpl Tests")
class ArtifactServiceImplTest {

    @Mock private ArtifactRepository artifactRepository;
    @Mock private BagClient          bagClient;
    @Mock private WalletClient       walletClient;
    @Mock private RoleClient         roleClient;
    @Mock private Random             random;

    @InjectMocks private ArtifactServiceImpl artifactService;

    private static final Long USER_ID        = 2001L;
    private static final int  ARTIFACT_INDEX = 1;
    private static final int  ARTIFACT_ID    = 10;

    /** Build a fully-populated Artifact for test use. */
    private Artifact buildArtifact(int level, int grade, boolean active) {
        Artifact a = new Artifact();
        a.setUserId(USER_ID);
        a.setArtifactIndex(ARTIFACT_INDEX);
        a.setArtifactId(ARTIFACT_ID);
        a.setLevel(level);
        a.setGrade(grade);
        a.setExp(0L);
        a.setIsActive(active);
        a.setIsEquipped(false);
        a.setRefinementLevel(0);
        a.setAwakeningStage(0);
        a.setSoulPower(0L);
        a.setDivineEssence(0L);
        a.setAttr1Type(1); a.setAttr1Value(100L);
        a.setAttr2Type(2); a.setAttr2Value(200L);
        a.setAttr3Type(3); a.setAttr3Value(300L);
        a.setAttr4Type(4); a.setAttr4Value(400L);
        a.setBlessingTier(0);
        return a;
    }

    // =========================================================
    // getAllArtifacts()
    // =========================================================
    @Nested
    @DisplayName("getAllArtifacts()")
    class GetAllArtifacts {

        @Test
        @DisplayName("TC-ARTIFACT-001 [P] Lay danh sach artifact – tra ve list day du")
        void getAllArtifacts_returnsListFromRepository() {
            Artifact a1 = buildArtifact(1, 1, true);
            Artifact a2 = buildArtifact(5, 2, false);
            given(artifactRepository.findByUserId(USER_ID)).willReturn(List.of(a1, a2));

            List<Artifact> result = artifactService.getAllArtifacts(USER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC-ARTIFACT-002 [P] Khong co artifact – tra ve danh sach rong")
        void getAllArtifacts_noArtifacts_returnsEmpty() {
            given(artifactRepository.findByUserId(USER_ID)).willReturn(List.of());

            List<Artifact> result = artifactService.getAllArtifacts(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // getArtifact()
    // =========================================================
    @Nested
    @DisplayName("getArtifact()")
    class GetArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-003 [P] Lay artifact ton tai – tra ve artifact dung")
        void getArtifact_found_returnsArtifact() {
            Artifact artifact = buildArtifact(10, 2, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            Artifact result = artifactService.getArtifact(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getLevel()).isEqualTo(10);
            assertThat(result.getGrade()).isEqualTo(2);
        }

        @Test
        @DisplayName("TC-ARTIFACT-004 [N] Artifact khong ton tai – nem ARTIFACT_NOT_FOUND")
        void getArtifact_notFound_throws() {
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> artifactService.getArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("ARTIFACT_NOT_FOUND");
        }
    }

    // =========================================================
    // unlockArtifact()
    // =========================================================
    @Nested
    @DisplayName("unlockArtifact()")
    class UnlockArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-005 [P] Mo khoa artifact moi – tao artifact voi 4 thuoc tinh ngau nhien")
        void unlockArtifact_newArtifact_createsWithRandomAttributes() {
            given(artifactRepository.findByUserIdAndArtifactId(USER_ID, ARTIFACT_ID))
                    .willReturn(Optional.empty());
            given(random.nextInt(anyInt())).willReturn(3);
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.unlockArtifact(USER_ID, ARTIFACT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getAttr1Type()).isNotNull();
            assertThat(result.getAttr2Type()).isNotNull();
            assertThat(result.getAttr3Type()).isNotNull();
            assertThat(result.getAttr4Type()).isNotNull();
            // No currency consumed for unlock
            then(walletClient).shouldHaveNoInteractions();
            then(bagClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-ARTIFACT-006 [N] Artifact da ton tai – nem ARTIFACT_ALREADY_EXISTS")
        void unlockArtifact_alreadyExists_throws() {
            Artifact existing = buildArtifact(1, 1, false);
            given(artifactRepository.findByUserIdAndArtifactId(USER_ID, ARTIFACT_ID))
                    .willReturn(Optional.of(existing));

            assertThatThrownBy(() -> artifactService.unlockArtifact(USER_ID, ARTIFACT_ID))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("ARTIFACT_ALREADY_EXISTS");
        }
    }

    // =========================================================
    // levelUpArtifact()
    // =========================================================
    @Nested
    @DisplayName("levelUpArtifact()")
    class LevelUpArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-007 [P] Tang cap artifact hop le – level tang 1, exp reset = 0 (khong tieu ton)")
        void levelUpArtifact_valid_incrementsLevelAndResetsExp() {
            Artifact artifact = buildArtifact(50, 5, true);
            artifact.setExp(500L);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.levelUpArtifact(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getLevel()).isEqualTo(51);
            assertThat(result.getExp()).isEqualTo(0);
            then(walletClient).shouldHaveNoInteractions();
            then(bagClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-ARTIFACT-008 [N] Artifact khong active – nem ARTIFACT_NOT_ACTIVE")
        void levelUpArtifact_notActive_throws() {
            Artifact artifact = buildArtifact(50, 5, false);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.levelUpArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("ARTIFACT_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-ARTIFACT-009 [N] Level toi da (100) – nem MAX_LEVEL_REACHED")
        void levelUpArtifact_atMaxLevel_throws() {
            Artifact artifact = buildArtifact(100, 5, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.levelUpArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("MAX_LEVEL_REACHED");
        }
    }

    // =========================================================
    // gradeUpArtifact()
    // =========================================================
    @Nested
    @DisplayName("gradeUpArtifact()")
    class GradeUpArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-010 [P] Tang phom artifact – grade tang 1, level reset = 1, exp = 0")
        void gradeUpArtifact_valid_incrementsGradeAndResetsLevel() {
            Artifact artifact = buildArtifact(100, 5, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.gradeUpArtifact(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getGrade()).isEqualTo(6);
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getExp()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-ARTIFACT-011 [N] Phom toi da (10) – nem MAX_GRADE_REACHED")
        void gradeUpArtifact_atMaxGrade_throws() {
            Artifact artifact = buildArtifact(100, 10, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.gradeUpArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("MAX_GRADE_REACHED");
        }
    }

    // =========================================================
    // equipArtifact() / unequipArtifact()
    // =========================================================
    @Nested
    @DisplayName("equipArtifact() / unequipArtifact()")
    class EquipUnequip {

        @Test
        @DisplayName("TC-ARTIFACT-012 [P] Trang bi artifact active – bo trang bi cu roi trang bi moi")
        void equipArtifact_active_unequipsAllThenSetsEquipped() {
            Artifact artifact = buildArtifact(1, 1, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            ArgumentCaptor<Artifact> captor = ArgumentCaptor.forClass(Artifact.class);
            given(artifactRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            artifactService.equipArtifact(USER_ID, ARTIFACT_INDEX);

            then(artifactRepository).should().unequipAllArtifacts(USER_ID);
            assertThat(captor.getValue().getIsEquipped()).isTrue();
        }

        @Test
        @DisplayName("TC-ARTIFACT-013 [N] Trang bi artifact khong active – nem ARTIFACT_NOT_ACTIVE")
        void equipArtifact_notActive_throws() {
            Artifact artifact = buildArtifact(1, 1, false);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.equipArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("ARTIFACT_NOT_ACTIVE");
        }

        @Test
        @DisplayName("TC-ARTIFACT-014 [P] Go trang bi – goi unequipAllArtifacts cho userId")
        void unequipArtifact_callsUnequipAll() {
            artifactService.unequipArtifact(USER_ID);

            then(artifactRepository).should().unequipAllArtifacts(USER_ID);
        }
    }

    // =========================================================
    // refineArtifact()
    // =========================================================
    @Nested
    @DisplayName("refineArtifact()")
    class RefineArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-015 [P] Tinh luyen hop le – refinementLevel tang 1")
        void refineArtifact_valid_incrementsRefinementLevel() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setRefinementLevel(7);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.refineArtifact(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getRefinementLevel()).isEqualTo(8);
        }

        @Test
        @DisplayName("TC-ARTIFACT-016 [N] Tinh luyen toi da (15) – nem MAX_REFINEMENT_REACHED")
        void refineArtifact_atMaxRefinement_throws() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setRefinementLevel(15);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.refineArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("MAX_REFINEMENT_REACHED");
        }
    }

    // =========================================================
    // awakenArtifact()
    // =========================================================
    @Nested
    @DisplayName("awakenArtifact()")
    class AwakenArtifact {

        @Test
        @DisplayName("TC-ARTIFACT-017 [P] Thuc tinh hop le – awakeningStage tang 1")
        void awakenArtifact_valid_incrementsAwakeningStage() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAwakeningStage(3);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.awakenArtifact(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getAwakeningStage()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-ARTIFACT-018 [N] Thuc tinh toi da (7) – nem MAX_AWAKENING_REACHED")
        void awakenArtifact_atMaxAwakening_throws() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAwakeningStage(7);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.awakenArtifact(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("MAX_AWAKENING_REACHED");
        }
    }

    // =========================================================
    // addSoulPower() / addDivineEssence()
    // =========================================================
    @Nested
    @DisplayName("addSoulPower() / addDivineEssence()")
    class SoulAndDivine {

        @Test
        @DisplayName("TC-ARTIFACT-019 [P] Them soul power – diem cong don tich luy")
        void addSoulPower_addsPointsCumulatively() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setSoulPower(500L);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.addSoulPower(USER_ID, ARTIFACT_INDEX, 200L);

            assertThat(result.getSoulPower()).isEqualTo(700L);
        }

        @Test
        @DisplayName("TC-ARTIFACT-020 [P] Them divine essence – luong cong don tich luy")
        void addDivineEssence_addsAmountCumulatively() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setDivineEssence(1000L);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.addDivineEssence(USER_ID, ARTIFACT_INDEX, 300L);

            assertThat(result.getDivineEssence()).isEqualTo(1300L);
        }
    }

    // =========================================================
    // upgradeBlessing()
    // =========================================================
    @Nested
    @DisplayName("upgradeBlessing()")
    class UpgradeBlessing {

        @Test
        @DisplayName("TC-ARTIFACT-021 [P] Nang cap blessing – blessingTier tang 1")
        void upgradeBlessing_valid_incrementsBlessingTier() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setBlessingTier(5);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.upgradeBlessing(USER_ID, ARTIFACT_INDEX);

            assertThat(result.getBlessingTier()).isEqualTo(6);
        }

        @Test
        @DisplayName("TC-ARTIFACT-022 [N] Blessing toi da (10) – nem MAX_BLESSING_REACHED")
        void upgradeBlessing_atMaxBlessing_throws() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setBlessingTier(10);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThatThrownBy(() -> artifactService.upgradeBlessing(USER_ID, ARTIFACT_INDEX))
                    .isInstanceOf(ArtifactServiceException.class)
                    .hasMessageContaining("MAX_BLESSING_REACHED");
        }
    }

    // =========================================================
    // refreshAttributes()
    // =========================================================
    @Nested
    @DisplayName("refreshAttributes()")
    class RefreshAttributes {

        @Test
        @DisplayName("TC-ARTIFACT-023 [P] lockFlag=0 – tat ca 4 thuoc tinh duoc lam moi")
        void refreshAttributes_lockFlagZero_refreshesAllAttributes() {
            Artifact artifact = buildArtifact(1, 1, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(random.nextInt(anyInt())).willReturn(5);
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.refreshAttributes(USER_ID, ARTIFACT_INDEX, 0);

            assertThat(result.getAttr1Type()).isNotNull();
            assertThat(result.getAttr2Type()).isNotNull();
            assertThat(result.getAttr3Type()).isNotNull();
            assertThat(result.getAttr4Type()).isNotNull();
            then(artifactRepository).should().save(any());
        }

        @Test
        @DisplayName("TC-ARTIFACT-024 [P] lockFlag=0b0001 (bit0=1) – attr1 bi khoa, attr2-4 duoc lam moi")
        void refreshAttributes_attr1Locked_keepsAttr1Unchanged() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAttr1Value(999L); // sentinel value
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(random.nextInt(anyInt())).willReturn(5);
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.refreshAttributes(USER_ID, ARTIFACT_INDEX, 1);

            // attr1 should NOT be refreshed (locked by bit 0)
            assertThat(result.getAttr1Value()).isEqualTo(999L);
        }

        @Test
        @DisplayName("TC-ARTIFACT-025 [P] lockFlag=0b1111=15 – tat ca attr bi khoa, khong co gi thay doi")
        void refreshAttributes_allLocked_noAttributesChanged() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAttr1Value(111L);
            artifact.setAttr2Value(222L);
            artifact.setAttr3Value(333L);
            artifact.setAttr4Value(444L);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));
            given(artifactRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Artifact result = artifactService.refreshAttributes(USER_ID, ARTIFACT_INDEX, 15);

            // All attrs locked, none should change
            assertThat(result.getAttr1Value()).isEqualTo(111L);
            assertThat(result.getAttr2Value()).isEqualTo(222L);
            assertThat(result.getAttr3Value()).isEqualTo(333L);
            assertThat(result.getAttr4Value()).isEqualTo(444L);
            then(random).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    // calculateArtifactPower()
    // =========================================================
    @Nested
    @DisplayName("calculateArtifactPower()")
    class CalculateArtifactPower {

        @Test
        @DisplayName("TC-ARTIFACT-026 [P] Artifact khong active – chieu chien = 0")
        void calculateArtifactPower_inactive_returnsZero() {
            Artifact artifact = buildArtifact(50, 5, false);

            Long power = artifactService.calculateArtifactPower(artifact);

            assertThat(power).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-ARTIFACT-027 [P] Artifact moi unlock (level 1, grade 1) – chieu chien = 200+1000+1000=2200")
        void calculateArtifactPower_newArtifact_returnsCorrectBasePower() {
            Artifact artifact = buildArtifact(1, 1, true);
            // level*200=200, grade*1000=1000, refine=0, awaken=0, soul=0, divine=0, bless=0
            // attr values: 100+200+300+400=1000 → total=200+1000+0+0+0+0+0+1000=2200

            Long power = artifactService.calculateArtifactPower(artifact);

            assertThat(power).isEqualTo(2200L);
        }

        @Test
        @DisplayName("TC-ARTIFACT-028 [P] Artifact day du cap nhat – tinh chieu chien theo cong thuc chinh xac")
        void calculateArtifactPower_fullyUpgraded_calculatesCorrectly() {
            Artifact artifact = buildArtifact(10, 2, true);
            artifact.setRefinementLevel(5);
            artifact.setAwakeningStage(2);
            artifact.setSoulPower(500L);
            artifact.setDivineEssence(100L);
            artifact.setBlessingTier(3);
            artifact.setAttr1Value(50L);
            artifact.setAttr2Value(75L);
            artifact.setAttr3Value(null);
            artifact.setAttr4Value(25L);
            // basePower  = 10*200    = 2000
            // gradePower = 2*1000    = 2000
            // refine     = 5*500     = 2500
            // awaken     = 2*2000    = 4000
            // soul       = 500/5     = 100
            // divine     = 100/10    = 10
            // bless      = 3*800     = 2400
            // attr       = 50+75+25  = 150
            // total = 2000+2000+2500+4000+100+10+2400+150 = 13160

            Long power = artifactService.calculateArtifactPower(artifact);

            assertThat(power).isEqualTo(13160L);
        }
    }

    // =========================================================
    // canLevelUp() / canGradeUp() / canRefine() / canAwaken()
    // =========================================================
    @Nested
    @DisplayName("canLevelUp() / canGradeUp() / canRefine() / canAwaken()")
    class CanUpgradeMethods {

        @Test
        @DisplayName("TC-ARTIFACT-029 [P] Level 50 < 100 – co the tang cap")
        void canLevelUp_belowMax_returnsTrue() {
            Artifact artifact = buildArtifact(50, 5, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canLevelUp(USER_ID, ARTIFACT_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ARTIFACT-030 [N] Level 100 = max – khong the tang cap")
        void canLevelUp_atMax_returnsFalse() {
            Artifact artifact = buildArtifact(100, 5, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canLevelUp(USER_ID, ARTIFACT_INDEX)).isFalse();
        }

        @Test
        @DisplayName("TC-ARTIFACT-031 [P] Grade 9 < 10 – co the tang phom")
        void canGradeUp_belowMax_returnsTrue() {
            Artifact artifact = buildArtifact(100, 9, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canGradeUp(USER_ID, ARTIFACT_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ARTIFACT-032 [N] Grade 10 = max – khong the tang phom")
        void canGradeUp_atMax_returnsFalse() {
            Artifact artifact = buildArtifact(100, 10, true);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canGradeUp(USER_ID, ARTIFACT_INDEX)).isFalse();
        }

        @Test
        @DisplayName("TC-ARTIFACT-033 [P] RefinementLevel 14 < 15 – co the tinh luyen")
        void canRefine_belowMax_returnsTrue() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setRefinementLevel(14);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canRefine(USER_ID, ARTIFACT_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ARTIFACT-034 [N] RefinementLevel 15 = max – khong the tinh luyen")
        void canRefine_atMax_returnsFalse() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setRefinementLevel(15);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canRefine(USER_ID, ARTIFACT_INDEX)).isFalse();
        }

        @Test
        @DisplayName("TC-ARTIFACT-035 [P] AwakeningStage 6 < 7 – co the thuc tinh")
        void canAwaken_belowMax_returnsTrue() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAwakeningStage(6);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canAwaken(USER_ID, ARTIFACT_INDEX)).isTrue();
        }

        @Test
        @DisplayName("TC-ARTIFACT-036 [N] AwakeningStage 7 = max – khong the thuc tinh")
        void canAwaken_atMax_returnsFalse() {
            Artifact artifact = buildArtifact(1, 1, true);
            artifact.setAwakeningStage(7);
            given(artifactRepository.findByUserIdAndArtifactIndex(USER_ID, ARTIFACT_INDEX))
                    .willReturn(Optional.of(artifact));

            assertThat(artifactService.canAwaken(USER_ID, ARTIFACT_INDEX)).isFalse();
        }
    }
}
