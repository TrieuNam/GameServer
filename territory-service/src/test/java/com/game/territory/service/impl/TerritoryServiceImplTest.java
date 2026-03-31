package com.game.territory.service.impl;

import com.SouthMillion.territory_service.client.BagClient;
import com.SouthMillion.territory_service.client.WalletClient;
import com.SouthMillion.territory_service.exception.TerritoryServiceException;
import com.SouthMillion.territory_service.model.entity.Territory;
import com.SouthMillion.territory_service.model.entity.TerritoryBuilding;
import com.SouthMillion.territory_service.repository.TerritoryBuildingRepository;
import com.SouthMillion.territory_service.repository.TerritoryRepository;
import com.SouthMillion.territory_service.service.impl.TerritoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TerritoryServiceImpl Tests")
class TerritoryServiceImplTest {

    @Mock private TerritoryRepository         territoryRepository;
    @Mock private TerritoryBuildingRepository buildingRepository;
    @Mock private BagClient                   bagClient;
    @Mock private WalletClient                walletClient;

    @InjectMocks private TerritoryServiceImpl territoryService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static Territory territory(Long userId, int level) {
        Territory t = new Territory();
        t.setUserId(userId);
        t.setTerritoryId(1);
        t.setLevel(level);
        t.setName("My Territory");
        t.setProsperity(0L);
        t.setDefenseRating(100L);
        t.setAttackRating(100L);
        t.setGoldProduction(100L);
        t.setResourceProduction(50L);
        t.setAccumulatedGold(0L);
        t.setAccumulatedResources(0L);
        t.setLastProductionTime(LocalDateTime.now());
        t.setMaxGoldStorage(10000L);
        t.setMaxResourceStorage(5000L);
        t.setBuildingSlots(10);
        t.setAppearanceId(1);
        return t;
    }

    private static TerritoryBuilding building(Long userId, int slotId, int status, int level) {
        TerritoryBuilding b = new TerritoryBuilding();
        b.setId(1L);
        b.setUserId(userId);
        b.setSlotId(slotId);
        b.setBuildingId(1001);
        b.setLevel(level);
        b.setStatus(status);
        b.setProductionRate(0L);
        b.setDefenseContribution(100L);
        b.setAttackContribution(50L);
        b.setProsperityContribution(10L);
        return b;
    }

    // =========================================================
    // getTerritory()
    // =========================================================
    @Nested
    @DisplayName("getTerritory()")
    class GetTerritory {

        @Test
        @DisplayName("TC-TERR-001 [P] Territory ton tai – tra ve entity")
        void getTerritory_found_returnsEntity() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            Territory result = territoryService.getTerritory(1L);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getLevel()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-TERR-002 [N] Territory khong ton tai – nem TERRITORY_NOT_FOUND")
        void getTerritory_notFound_throws() {
            given(territoryRepository.findByUserId(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> territoryService.getTerritory(99L))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("not found");
        }
    }

    // =========================================================
    // createTerritory()
    // =========================================================
    @Nested
    @DisplayName("createTerritory()")
    class CreateTerritory {

        @Test
        @DisplayName("TC-TERR-003 [P] Tao territory moi – khoi tao voi cac gia tri mac dinh")
        void createTerritory_notExists_createsWithDefaults() {
            given(territoryRepository.existsByUserId(1L)).willReturn(false);
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Territory result = territoryService.createTerritory(1L, 1);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getBuildingSlots()).isEqualTo(10);
            assertThat(result.getMaxGoldStorage()).isEqualTo(10000L);
            assertThat(result.getAccumulatedGold()).isEqualTo(0L);
            assertThat(result.getDefenseRating()).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-TERR-004 [N] Territory da ton tai – nem TERRITORY_EXISTS")
        void createTerritory_alreadyExists_throws() {
            given(territoryRepository.existsByUserId(1L)).willReturn(true);

            assertThatThrownBy(() -> territoryService.createTerritory(1L, 1))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // =========================================================
    // levelUpTerritory()
    // =========================================================
    @Nested
    @DisplayName("levelUpTerritory()")
    class LevelUpTerritory {

        @Test
        @DisplayName("TC-TERR-005 [P] Level up hop le – tru gold va materials, tang level")
        void levelUpTerritory_valid_incrementsLevel() {
            Territory t = territory(1L, 3);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Territory result = territoryService.levelUpTerritory(1L);

            assertThat(result.getLevel()).isEqualTo(4);
            assertThat(result.getBuildingSlots()).isEqualTo(11); // slots + 1
            assertThat(result.getMaxGoldStorage()).isEqualTo(11000L); // +1000
            // gold cost = 1000 * 3 = 3000
            then(walletClient).should().consumeCurrency(eq("1"), any());
            then(bagClient).should().useItem(eq("1"), any());
        }

        @Test
        @DisplayName("TC-TERR-006 [N] Da o level toi da (50) – nem MAX_TERRITORY_LEVEL")
        void levelUpTerritory_maxLevel_throws() {
            Territory t = territory(1L, 50);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.levelUpTerritory(1L))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("max level");
        }
    }

    // =========================================================
    // renameTerritory()
    // =========================================================
    @Nested
    @DisplayName("renameTerritory()")
    class RenameTerritory {

        @Test
        @DisplayName("TC-TERR-007 [P] Ten hop le – doi ten territory")
        void renameTerritory_valid_updatesName() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Territory result = territoryService.renameTerritory(1L, "Dragon Kingdom");

            assertThat(result.getName()).isEqualTo("Dragon Kingdom");
        }

        @Test
        @DisplayName("TC-TERR-008 [N] Ten null – nem INVALID_NAME")
        void renameTerritory_nullName_throws() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.renameTerritory(1L, null))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("TC-TERR-009 [N] Ten rong – nem INVALID_NAME")
        void renameTerritory_emptyName_throws() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.renameTerritory(1L, "  "))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("TC-TERR-010 [N] Ten qua 50 ky tu – nem INVALID_NAME")
        void renameTerritory_tooLongName_throws() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            String longName = "A".repeat(51);

            assertThatThrownBy(() -> territoryService.renameTerritory(1L, longName))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("Invalid");
        }
    }

    // =========================================================
    // changeAppearance()
    // =========================================================
    @Nested
    @DisplayName("changeAppearance()")
    class ChangeAppearance {

        @Test
        @DisplayName("TC-TERR-011 [P] ID ngoai hinh hop le – cap nhat appearanceId")
        void changeAppearance_valid_updatesAppearance() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Territory result = territoryService.changeAppearance(1L, 5);

            assertThat(result.getAppearanceId()).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-TERR-012 [N] ID ngoai hinh < 1 – nem INVALID_APPEARANCE")
        void changeAppearance_invalidId_throws() {
            Territory t = territory(1L, 5);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.changeAppearance(1L, 0))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("Invalid");
        }
    }

    // =========================================================
    // collectResources()
    // =========================================================
    @Nested
    @DisplayName("collectResources()")
    class CollectResources {

        @Test
        @DisplayName("TC-TERR-013 [P] Co tai nguyen tich luy – thu thap va reset ve 0")
        void collectResources_hasAccumulated_collectsAndResets() {
            Territory t = territory(1L, 5);
            t.setAccumulatedGold(500L);
            t.setAccumulatedResources(200L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Territory result = territoryService.collectResources(1L);

            assertThat(result.getAccumulatedGold()).isEqualTo(0L);
            assertThat(result.getAccumulatedResources()).isEqualTo(0L);
            then(walletClient).should(atLeastOnce()).addCurrency(eq("1"), any());
        }

        @Test
        @DisplayName("TC-TERR-014 [N] Khong co tai nguyen – nem NO_RESOURCES")
        void collectResources_noAccumulated_throws() {
            Territory t = territory(1L, 5);
            t.setAccumulatedGold(0L);
            t.setAccumulatedResources(0L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.collectResources(1L))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("No resources");
        }
    }

    // =========================================================
    // constructBuilding()
    // =========================================================
    @Nested
    @DisplayName("constructBuilding()")
    class ConstructBuilding {

        @Test
        @DisplayName("TC-TERR-015 [P] Slot hop le va trong – bat dau xay dung, tru gold va materials")
        void constructBuilding_validSlot_startsConstruction() {
            Territory t = territory(1L, 5);
            t.setBuildingSlots(10);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.existsByUserIdAndSlotId(1L, 1)).willReturn(false);
            given(buildingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TerritoryBuilding result = territoryService.constructBuilding(1L, 1, 1001);

            assertThat(result.getStatus()).isEqualTo(1); // CONSTRUCTING
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getFinishTime()).isNotNull();
            // gold cost = 5000
            then(walletClient).should().consumeCurrency(eq("1"), any());
            // materials: wood and stone
            then(bagClient).should(times(2)).useItem(eq("1"), any());
        }

        @Test
        @DisplayName("TC-TERR-016 [N] Slot ngoai pham vi – nem INVALID_SLOT")
        void constructBuilding_invalidSlot_throws() {
            Territory t = territory(1L, 5);
            t.setBuildingSlots(10);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThatThrownBy(() -> territoryService.constructBuilding(1L, 11, 1001))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("TC-TERR-017 [N] Slot da co cong trinh – nem SLOT_OCCUPIED")
        void constructBuilding_slotOccupied_throws() {
            Territory t = territory(1L, 5);
            t.setBuildingSlots(10);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.existsByUserIdAndSlotId(1L, 1)).willReturn(true);

            assertThatThrownBy(() -> territoryService.constructBuilding(1L, 1, 1001))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("occupied");
        }
    }

    // =========================================================
    // upgradeBuilding()
    // =========================================================
    @Nested
    @DisplayName("upgradeBuilding()")
    class UpgradeBuilding {

        @Test
        @DisplayName("TC-TERR-018 [P] Cong trinh da hoan thanh – bat dau nang cap, tru gold")
        void upgradeBuilding_builtBuilding_startsUpgrade() {
            TerritoryBuilding b = building(1L, 1, 2, 3); // status=BUILT, level=3
            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));
            given(buildingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            TerritoryBuilding result = territoryService.upgradeBuilding(1L, 1);

            assertThat(result.getStatus()).isEqualTo(3); // UPGRADING
            // gold cost = 2000 * 3 = 6000
            then(walletClient).should().consumeCurrency(eq("1"), any());
            then(bagClient).should().useItem(eq("1"), any());
        }

        @Test
        @DisplayName("TC-TERR-019 [N] Cong trinh dang xay – nem BUILDING_NOT_BUILT")
        void upgradeBuilding_constructing_throws() {
            TerritoryBuilding b = building(1L, 1, 1, 1); // status=CONSTRUCTING
            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));

            assertThatThrownBy(() -> territoryService.upgradeBuilding(1L, 1))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("not ready");
        }
    }

    // =========================================================
    // finishConstruction()
    // =========================================================
    @Nested
    @DisplayName("finishConstruction()")
    class FinishConstruction {

        @Test
        @DisplayName("TC-TERR-020 [P] Xay dung xong – chuyen trang thai sang BUILT")
        void finishConstruction_constructionDone_setsBuilt() {
            TerritoryBuilding b = building(1L, 1, 1, 1); // CONSTRUCTING, level 1
            b.setFinishTime(LocalDateTime.now().minusMinutes(1)); // past finish time
            Territory t = territory(1L, 5);

            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));
            given(buildingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(buildingRepository.getTotalDefense(1L)).willReturn(null);
            given(buildingRepository.getTotalAttack(1L)).willReturn(null);
            given(buildingRepository.getTotalProsperity(1L)).willReturn(null);
            given(buildingRepository.getTotalProductionRate(1L)).willReturn(null);

            TerritoryBuilding result = territoryService.finishConstruction(1L, 1);

            assertThat(result.getStatus()).isEqualTo(2); // BUILT
            assertThat(result.getFinishTime()).isNull();
            assertThat(result.getDefenseContribution()).isEqualTo(100L); // 100 * level 1
        }

        @Test
        @DisplayName("TC-TERR-021 [P] Nang cap xong – chuyen sang BUILT va tang level")
        void finishConstruction_upgradeDone_incrementsLevel() {
            TerritoryBuilding b = building(1L, 1, 3, 2); // UPGRADING, level 2
            b.setFinishTime(LocalDateTime.now().minusMinutes(1));
            Territory t = territory(1L, 5);

            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));
            given(buildingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(buildingRepository.getTotalDefense(1L)).willReturn(null);
            given(buildingRepository.getTotalAttack(1L)).willReturn(null);
            given(buildingRepository.getTotalProsperity(1L)).willReturn(null);
            given(buildingRepository.getTotalProductionRate(1L)).willReturn(null);

            TerritoryBuilding result = territoryService.finishConstruction(1L, 1);

            assertThat(result.getLevel()).isEqualTo(3); // 2 + 1
            assertThat(result.getStatus()).isEqualTo(2); // BUILT
            assertThat(result.getDefenseContribution()).isEqualTo(300L); // 100 * 3
        }

        @Test
        @DisplayName("TC-TERR-022 [N] Cong trinh chua hoan thanh (finishTime tuong lai) – nem CONSTRUCTION_NOT_FINISHED")
        void finishConstruction_notFinishedYet_throws() {
            TerritoryBuilding b = building(1L, 1, 1, 1);
            b.setFinishTime(LocalDateTime.now().plusHours(1)); // future finish time

            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));

            assertThatThrownBy(() -> territoryService.finishConstruction(1L, 1))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("not finished");
        }

        @Test
        @DisplayName("TC-TERR-023 [N] Cong trinh khong dang xay – nem NO_CONSTRUCTION")
        void finishConstruction_noConstruction_throws() {
            TerritoryBuilding b = building(1L, 1, 2, 1); // status=BUILT

            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));

            assertThatThrownBy(() -> territoryService.finishConstruction(1L, 1))
                    .isInstanceOf(TerritoryServiceException.class)
                    .hasMessageContaining("No construction");
        }
    }

    // =========================================================
    // demolishBuilding()
    // =========================================================
    @Nested
    @DisplayName("demolishBuilding()")
    class DemolishBuilding {

        @Test
        @DisplayName("TC-TERR-024 [P] Pha huy cong trinh – xoa va hoan tra 2500 gold")
        void demolishBuilding_valid_deletesAndRefunds() {
            TerritoryBuilding b = building(1L, 1, 2, 3);
            Territory t = territory(1L, 5);

            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(territoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(buildingRepository.getTotalDefense(1L)).willReturn(null);
            given(buildingRepository.getTotalAttack(1L)).willReturn(null);
            given(buildingRepository.getTotalProsperity(1L)).willReturn(null);
            given(buildingRepository.getTotalProductionRate(1L)).willReturn(null);

            territoryService.demolishBuilding(1L, 1);

            then(buildingRepository).should().delete(b);
            // refund = 2500 gold
            then(walletClient).should().addCurrency(eq("1"), any());
        }
    }

    // =========================================================
    // getTotalDefense() / getTotalAttack() / getTotalProsperity()
    // =========================================================
    @Nested
    @DisplayName("Stats queries")
    class StatsQueries {

        @Test
        @DisplayName("TC-TERR-025 [P] Tong chien phong = territory base + building defense")
        void getTotalDefense_combinesTotalWithBuildings() {
            Territory t = territory(1L, 5);
            t.setDefenseRating(100L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.getTotalDefense(1L)).willReturn(500L);

            Long result = territoryService.getTotalDefense(1L);

            assertThat(result).isEqualTo(600L); // 100 + 500
        }

        @Test
        @DisplayName("TC-TERR-026 [P] Building defense null – chi tra ve territory defense")
        void getTotalDefense_nullBuildingDefense_returnsTerritoryOnly() {
            Territory t = territory(1L, 5);
            t.setDefenseRating(100L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.getTotalDefense(1L)).willReturn(null);

            Long result = territoryService.getTotalDefense(1L);

            assertThat(result).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-TERR-027 [P] Tong tan cong = territory base + building attack")
        void getTotalAttack_combinesTotalWithBuildings() {
            Territory t = territory(1L, 5);
            t.setAttackRating(100L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.getTotalAttack(1L)).willReturn(300L);

            Long result = territoryService.getTotalAttack(1L);

            assertThat(result).isEqualTo(400L);
        }

        @Test
        @DisplayName("TC-TERR-028 [P] Tong prosperity = territory base + building prosperity")
        void getTotalProsperity_combinesTotalWithBuildings() {
            Territory t = territory(1L, 5);
            t.setProsperity(50L);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.getTotalProsperity(1L)).willReturn(200L);

            Long result = territoryService.getTotalProsperity(1L);

            assertThat(result).isEqualTo(250L);
        }
    }

    // =========================================================
    // canLevelUp() / canConstruct() / canUpgradeBuilding()
    // =========================================================
    @Nested
    @DisplayName("Validation methods")
    class Validation {

        @Test
        @DisplayName("TC-TERR-029 [P] Level chua max – canLevelUp tra ve true")
        void canLevelUp_belowMax_returnsTrue() {
            Territory t = territory(1L, 30);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThat(territoryService.canLevelUp(1L)).isTrue();
        }

        @Test
        @DisplayName("TC-TERR-030 [P] Level da max (50) – canLevelUp tra ve false")
        void canLevelUp_atMax_returnsFalse() {
            Territory t = territory(1L, 50);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));

            assertThat(territoryService.canLevelUp(1L)).isFalse();
        }

        @Test
        @DisplayName("TC-TERR-031 [P] Slot trong va hop le – canConstruct tra ve true")
        void canConstruct_emptyValidSlot_returnsTrue() {
            Territory t = territory(1L, 5);
            t.setBuildingSlots(10);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.existsByUserIdAndSlotId(1L, 3)).willReturn(false);

            assertThat(territoryService.canConstruct(1L, 3)).isTrue();
        }

        @Test
        @DisplayName("TC-TERR-032 [P] Slot da co cong trinh – canConstruct tra ve false")
        void canConstruct_occupiedSlot_returnsFalse() {
            Territory t = territory(1L, 5);
            t.setBuildingSlots(10);
            given(territoryRepository.findByUserId(1L)).willReturn(Optional.of(t));
            given(buildingRepository.existsByUserIdAndSlotId(1L, 1)).willReturn(true);

            assertThat(territoryService.canConstruct(1L, 1)).isFalse();
        }

        @Test
        @DisplayName("TC-TERR-033 [P] Cong trinh o BUILT – canUpgradeBuilding tra ve true")
        void canUpgradeBuilding_builtBuilding_returnsTrue() {
            TerritoryBuilding b = building(1L, 1, 2, 3); // BUILT
            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));

            assertThat(territoryService.canUpgradeBuilding(1L, 1)).isTrue();
        }

        @Test
        @DisplayName("TC-TERR-034 [P] Cong trinh dang xay – canUpgradeBuilding tra ve false")
        void canUpgradeBuilding_constructing_returnsFalse() {
            TerritoryBuilding b = building(1L, 1, 1, 1); // CONSTRUCTING
            given(buildingRepository.findByUserIdAndSlotId(1L, 1)).willReturn(Optional.of(b));

            assertThat(territoryService.canUpgradeBuilding(1L, 1)).isFalse();
        }
    }
}
