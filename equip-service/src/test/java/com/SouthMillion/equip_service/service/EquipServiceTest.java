package com.SouthMillion.equip_service.service;

import com.SouthMillion.equip_service.config.EquipProperties;
import com.SouthMillion.equip_service.config.EquipmentConfigCache;
import com.SouthMillion.equip_service.entity.EquipSlotEntity;
import com.SouthMillion.equip_service.repository.EquipSnapshotRepository;
import com.SouthMillion.equip_service.repository.EquipSlotRepository;
import com.SouthMillion.equip_service.service.client.BagInternalFeign;
import com.SouthMillion.equip_service.service.client.BagPublicFeign;
import com.SouthMillion.equip_service.service.client.ItemMetaFeign;
import com.SouthMillion.equip_service.service.client.RoleFeign;
import io.micrometer.core.instrument.MeterRegistry;
import org.SouthMillion.dto.bag.*;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EquipService Tests")
class EquipServiceTest {

    @Mock
    private EquipSlotRepository slotRepo;

    @Mock
    private ItemMetaFeign itemMetaFeign;

    @Mock
    private BagInternalFeign bagFeign;

    @Mock
    private BagPublicFeign bagPublicFeign;

    @Mock
    private EquipProperties props;

    @Mock
    private EquipmentConfigCache equipmentConfigCache;

    @Mock
    private RoleFeign roleFeign;

    @Mock
    private EquipSnapshotRepository snapshotRepo;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private EquipService equipService;

    private static final Long ROLE_ID = 1L;
    private static final int ITEM_ID = 100;
    private static final int EQUIP_TYPE = 1; // weapon slot

    // Helper: meta map with equipType
    private Map<String, Object> metaWith(int equipType) {
        return Map.of("equipType", equipType);
    }

    private Map<String, Object> metaEmpty() {
        return Map.of();
    }

    // =========================================================
    // list
    // =========================================================
    @Nested
    @DisplayName("list()")
    class ListEquips {

        @Test
        @DisplayName("TC-EQP-001 [P] Lay danh sach trang bi dang mac – tra ve dung")
        void list_returnsEquippedItems() {
            EquipSlotEntity slot = new EquipSlotEntity();
            slot.setRoleId(ROLE_ID);
            slot.setEquipType(1);
            slot.setItemId(ITEM_ID);
            slot.setHp(100);
            slot.setAttack(50);

            given(slotRepo.findByRoleId(ROLE_ID)).willReturn(List.of(slot));

            EquipDTOs.ListResp resp = equipService.list(ROLE_ID);

            assertThat(resp.getItems()).hasSize(1);
            assertThat(resp.getItems().get(0).getItemId()).isEqualTo(ITEM_ID);
        }

        @Test
        @DisplayName("TC-EQP-002 [P] Chua mac gi – tra ve danh sach rong")
        void list_noEquips_returnsEmpty() {
            given(slotRepo.findByRoleId(ROLE_ID)).willReturn(List.of());

            EquipDTOs.ListResp resp = equipService.list(ROLE_ID);

            assertThat(resp.getItems()).isEmpty();
        }
    }

    // =========================================================
    // equip
    // =========================================================
    @Nested
    @DisplayName("equip()")
    class Equip {

        private EquipDTOs.EquipReq equipReq(Long roleId, int itemId) {
            EquipDTOs.EquipReq req = new EquipDTOs.EquipReq();
            req.setRoleId(String.valueOf(roleId));
            req.setItemId(itemId);
            return req;
        }

        @Test
        @DisplayName("TC-EQP-010 [N] Item khong the mac (equipType=0) – tra ve NG(ITEM_NOT_EQUIPPABLE)")
        void equip_notEquippable_returnsNG() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(metaEmpty());

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.message()).isEqualTo("ITEM_NOT_EQUIPPABLE");
        }

        @Test
        @DisplayName("TC-EQP-011 [N] Khong du item trong tui – tra ve NG(ITEM_NOT_ENOUGH)")
        void equip_insufficientItem_returnsNG() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(metaWith(EQUIP_TYPE));
            given(bagFeign.consume(any(BagConsumeReq.class)))
                    .willReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.message()).isEqualTo("ITEM_NOT_ENOUGH");
        }

        @Test
        @DisplayName("TC-EQP-012 [P] Mac trang bi vao slot trong – thanh cong")
        void equip_emptySlot_success() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(metaWith(EQUIP_TYPE));
            given(bagFeign.consume(any(BagConsumeReq.class)))
                    .willReturn(ResponseEntity.noContent().build());

            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE)).willReturn(Optional.empty());
            given(slotRepo.save(any(EquipSlotEntity.class))).willAnswer(inv -> inv.getArgument(0));

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isTrue();
            then(slotRepo).should().save(argThat(s -> s.getItemId() == ITEM_ID));
        }

        @Test
        @DisplayName("TC-EQP-012A [P] frist_att/second_att duoc resolve thanh attr thuc te khi mac")
        void equip_resolvesAttrGroupsFromConfig() {
            EquipmentConfigCache.EquipRow row = new EquipmentConfigCache.EquipRow();
            row.id = ITEM_ID;
            row.part = EQUIP_TYPE;
            row.hp_max = 120;
            row.att_max = 45;
            row.def_max = 18;
            row.speed_max = 9;
            row.frist_att = 4;
            row.second_att = 5;

            given(equipmentConfigCache.find(ITEM_ID)).willReturn(Optional.of(row));
            given(equipmentConfigCache.resolveColorAttr(4))
                    .willReturn(Optional.of(new EquipmentConfigCache.ColorAttrBonus(4, 11, 200)));
            given(equipmentConfigCache.resolveColorAttr(5))
                    .willReturn(Optional.of(new EquipmentConfigCache.ColorAttrBonus(5, 25, 1)));
            given(bagFeign.consume(any(BagConsumeReq.class)))
                    .willReturn(ResponseEntity.noContent().build());
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE)).willReturn(Optional.empty());
            given(slotRepo.save(any(EquipSlotEntity.class))).willAnswer(inv -> inv.getArgument(0));

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isTrue();
            then(slotRepo).should().save(argThat(s -> s.getItemId() == ITEM_ID
                    && s.getAttrType1() == 11
                    && s.getAttrValue1() == 200
                    && s.getAttrType2() == 25
                    && s.getAttrValue2() == 1));
        }

        @Test
        @DisplayName("TC-EQP-013 [P] Mac trang bi vao slot co do cu – tra do cu ve tui")
        void equip_slotHasOldItem_returnsOldToBag() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(metaWith(EQUIP_TYPE));
            given(bagFeign.consume(any(BagConsumeReq.class)))
                    .willReturn(ResponseEntity.noContent().build());
            given(itemMetaFeign.meta(99)).willReturn(Map.of("equipType", EQUIP_TYPE, "quality", 4));

            // Existing item in slot
            EquipSlotEntity existingSlot = new EquipSlotEntity();
            existingSlot.setRoleId(ROLE_ID);
            existingSlot.setEquipType(EQUIP_TYPE);
            existingSlot.setItemId(99); // old item
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE))
                    .willReturn(Optional.of(existingSlot));

            given(props.getEquipBagType()).willReturn((byte) 6);

            given(bagFeign.add(any(BagAddItemReq.class)))
                    .willReturn(ResponseEntity.ok(List.of()));
            given(slotRepo.save(any(EquipSlotEntity.class))).willAnswer(inv -> inv.getArgument(0));

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isTrue();
            ArgumentCaptor<BagAddItemReq> captor = ArgumentCaptor.forClass(BagAddItemReq.class);
            then(bagFeign).should().add(captor.capture());
            BagAddItemReq.Item returned = captor.getValue().getItems().getFirst();
            assertThat(returned.getQuality()).isEqualTo(4);
            assertThat(returned.getBagType()).isEqualTo(6);
        }

        @Test
        @DisplayName("TC-EQP-014 [P] equipType=0 la hop le theo config slot 0")
        void equip_slotZero_isValidAndEquips() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(metaWith(0));
            given(bagFeign.consume(any(BagConsumeReq.class)))
                    .willReturn(ResponseEntity.noContent().build());
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, 0)).willReturn(Optional.empty());
            given(slotRepo.save(any(EquipSlotEntity.class))).willAnswer(inv -> inv.getArgument(0));

            EquipDTOs.OkResp resp = equipService.equip(equipReq(ROLE_ID, ITEM_ID));

            assertThat(resp.ok()).isTrue();
            then(slotRepo).should().save(argThat(s -> s.getEquipType() == 0 && s.getItemId() == ITEM_ID));
        }
    }

    // =========================================================
    // unequip
    // =========================================================
    @Nested
    @DisplayName("unequip()")
    class Unequip {

        private EquipDTOs.UnequipReq unequipReq(Long roleId, int equipType) {
            EquipDTOs.UnequipReq req = new EquipDTOs.UnequipReq();
            req.setRoleId(String.valueOf(roleId));
            req.setEquipType(equipType);
            return req;
        }

        @Test
        @DisplayName("TC-EQP-020 [N] Slot trong – tra ve NG(SLOT_EMPTY)")
        void unequip_emptySlot_returnsNG() {
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE)).willReturn(Optional.empty());

            EquipDTOs.OkResp resp = equipService.unequip(unequipReq(ROLE_ID, EQUIP_TYPE));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.message()).isEqualTo("SLOT_EMPTY");
        }

        @Test
        @DisplayName("TC-EQP-021 [N] Slot co item nhung itemId=0 – tra ve NG(SLOT_EMPTY)")
        void unequip_slotWithZeroItemId_returnsNG() {
            EquipSlotEntity emptySlot = new EquipSlotEntity();
            emptySlot.setItemId(0);
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE))
                    .willReturn(Optional.of(emptySlot));

            EquipDTOs.OkResp resp = equipService.unequip(unequipReq(ROLE_ID, EQUIP_TYPE));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.message()).isEqualTo("SLOT_EMPTY");
        }

        @Test
        @DisplayName("TC-EQP-022 [N] Them vao tui that bai – tra ve NG(BAG_ADD_FAILED)")
        void unequip_bagAddFailed_returnsNG() {
            EquipSlotEntity slot = new EquipSlotEntity();
            slot.setItemId(ITEM_ID);
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE))
                    .willReturn(Optional.of(slot));

            given(bagFeign.add(any(BagAddItemReq.class)))
                    .willReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

            EquipDTOs.OkResp resp = equipService.unequip(unequipReq(ROLE_ID, EQUIP_TYPE));

            assertThat(resp.ok()).isFalse();
            assertThat(resp.message()).isEqualTo("BAG_ADD_FAILED");
        }

        @Test
        @DisplayName("TC-EQP-023 [P] Thao trang bi – slot trong va item tra ve tui")
        void unequip_success() {
            EquipSlotEntity slot = new EquipSlotEntity();
            slot.setRoleId(ROLE_ID);
            slot.setEquipType(EQUIP_TYPE);
            slot.setItemId(ITEM_ID);
            given(slotRepo.findByRoleIdAndEquipType(ROLE_ID, EQUIP_TYPE))
                    .willReturn(Optional.of(slot));

            given(itemMetaFeign.meta(ITEM_ID)).willReturn(Map.of("equipType", EQUIP_TYPE, "quality", 7));

            given(bagFeign.add(any(BagAddItemReq.class)))
                    .willReturn(ResponseEntity.ok(List.of()));
            given(slotRepo.save(any(EquipSlotEntity.class))).willAnswer(inv -> inv.getArgument(0));

            EquipDTOs.UnequipReq req = unequipReq(ROLE_ID, EQUIP_TYPE);
            req.setBagType((byte) 2);
            EquipDTOs.OkResp resp = equipService.unequip(req);

            assertThat(resp.ok()).isTrue();
            then(slotRepo).should().save(argThat(s -> s.getItemId() == 0)); // slot cleared
            ArgumentCaptor<BagAddItemReq> captor = ArgumentCaptor.forClass(BagAddItemReq.class);
            then(bagFeign).should().add(captor.capture());
            BagAddItemReq.Item returned = captor.getValue().getItems().getFirst();
            assertThat(returned.getQuality()).isEqualTo(7);
            assertThat(returned.getBagType()).isEqualTo(2);
        }
    }

    // =========================================================
    // computeSell
    // =========================================================
    @Nested
    @DisplayName("computeSell()")
    class ComputeSell {

        @Test
        @DisplayName("TC-EQP-030 [P] Tinh gia ban theo meta – dung gia")
        void computeSell_withMeta_returnsMetaPrice() {
            given(itemMetaFeign.meta(ITEM_ID))
                    .willReturn(Map.of("sell_price", 500, "sell_exp", 100));

            Map<String, Object> req = Map.of("item", Map.of("itemId", ITEM_ID, "quality", 2, "equipLevel", 5));
            Map<String, Object> result = equipService.computeSell(req);

            assertThat(result.get("coin")).isEqualTo(500L);
            assertThat(result.get("exp")).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-EQP-031 [P] Tinh gia ban voi businessman bonus")
        void computeSell_withBusinessmanBonus_increasesCoin() {
            given(itemMetaFeign.meta(ITEM_ID))
                    .willReturn(Map.of("sell_price", 1000));

            // businessman = 1000 permyriad = 10% bonus -> 1000 * 1.1 = 1100
            Map<String, Object> req = Map.of(
                    "item", Map.of("itemId", ITEM_ID, "quality", 1, "equipLevel", 1),
                    "businessmanPermyriad", 1000L
            );
            Map<String, Object> result = equipService.computeSell(req);

            assertThat((Long) result.get("coin")).isEqualTo(1100L);
        }

        @Test
        @DisplayName("TC-EQP-032 [P] Khong co meta – dung fallback cong thuc")
        void computeSell_noMeta_usesFallback() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(Map.of());
            // props return 0 -> Math.max(1, 0) = 1
            given(props.getSellCoinBase()).willReturn(0L);
            given(props.getSellCoinPerQuality()).willReturn(0L);
            given(props.getSellCoinPerLevel()).willReturn(0L);
            given(props.getSellExpBase()).willReturn(0L);
            given(props.getSellExpPerQuality()).willReturn(0L);
            given(props.getSellExpPerLevel()).willReturn(0L);

            Map<String, Object> req = Map.of("item", Map.of("itemId", ITEM_ID, "quality", 2, "equipLevel", 3));
            Map<String, Object> result = equipService.computeSell(req);

            // coin > 0 since there's always a fallback
            assertThat((Long) result.get("coin")).isPositive();
        }
    }

    // =========================================================
    // decompose
    // =========================================================
    @Nested
    @DisplayName("decompose()")
    class Decompose {

        @Test
        @DisplayName("TC-EQP-040 [P] Phan giai trang bi – tra ve dung ket qua")
        void decompose_withMeta_returnsCorrectResult() {
            given(itemMetaFeign.meta(ITEM_ID))
                    .willReturn(Map.of(
                            "decompose_item_id", 999,
                            "decompose_num_base", 10,
                            "decompose_num_per_level", 2,
                            "decompose_exp", 50
                    ));

            Map<String, Object> req = Map.of("item", Map.of("itemId", ITEM_ID, "quality", 2, "equipLevel", 3));
            Map<String, Object> result = equipService.decompose(req);

            assertThat(result.get("itemId")).isEqualTo(999);
            // num = base(10) + perLevel(2) * (level-1=2) = 10 + 4 = 14
            assertThat(result.get("num")).isEqualTo(14L);
            assertThat(result.get("exp")).isEqualTo(50L);
        }

        @Test
        @DisplayName("TC-EQP-041 [P] Khong co meta phan giai – dung fallback tu properties")
        void decompose_noMeta_usesFallback() {
            given(itemMetaFeign.meta(ITEM_ID)).willReturn(Map.of());
            given(props.getDecomposeItemId()).willReturn(888);
            given(props.getDecomposeNumBase()).willReturn(5L);
            given(props.getDecomposeNumPerLevel()).willReturn(1L);
            given(props.getSellExpBase()).willReturn(0L);
            given(props.getSellExpPerQuality()).willReturn(0L);
            given(props.getSellExpPerLevel()).willReturn(0L);

            Map<String, Object> req = Map.of("item", Map.of("itemId", ITEM_ID, "quality", 1, "equipLevel", 1));
            Map<String, Object> result = equipService.decompose(req);

            assertThat(result.get("itemId")).isEqualTo(888);
            assertThat((Long) result.get("num")).isGreaterThan(0L);
        }
    }

    // =========================================================
    // resolveItemId
    // =========================================================
    @Nested
    @DisplayName("resolveItemId()")
    class ResolveItemId {

        @Test
        @DisplayName("TC-EQP-050 [P] Resolve itemId theo equipType/quality/level tu equipment cache")
        void resolveItemId_matchesFromEquipmentConfigCache() {
            EquipmentConfigCache.EquipRow r1 = new EquipmentConfigCache.EquipRow();
            r1.id = 50001;
            r1.part = 1;
            r1.quality = 2;
            r1.level = 10;

            EquipmentConfigCache.EquipRow r2 = new EquipmentConfigCache.EquipRow();
            r2.id = 50002;
            r2.part = 1;
            r2.quality = 3;
            r2.level = 10;

            given(equipmentConfigCache.allRows()).willReturn(List.of(r1, r2));

            Map<String, Object> result = equipService.resolveItemId(
                    Map.of("equipType", 1, "quality", 3, "level", 10)
            );

            assertThat(result).containsEntry("itemId", 50002);
        }

        @Test
        @DisplayName("TC-EQP-051 [N] Khong co match thi tra map rong")
        void resolveItemId_noMatch_returnsEmpty() {
            given(equipmentConfigCache.allRows()).willReturn(List.of());

            Map<String, Object> result = equipService.resolveItemId(
                    Map.of("equipType", 9, "quality", 1, "level", 1)
            );

            assertThat(result).isEmpty();
        }
    }
}


