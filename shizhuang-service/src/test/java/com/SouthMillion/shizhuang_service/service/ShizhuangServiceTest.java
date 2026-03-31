package com.SouthMillion.shizhuang_service.service;

import com.SouthMillion.shizhuang_service.client.WalletFeignClient;
import com.SouthMillion.shizhuang_service.entity.PlayerShizhuang;
import com.SouthMillion.shizhuang_service.repository.PlayerShizhuangRepository;
import org.SouthMillion.dto.ShiZhuang.ShizhuangDTOs;
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
@DisplayName("ShizhuangService Tests")
class ShizhuangServiceTest {

    @Mock private PlayerShizhuangRepository repository;
    @Mock private WalletFeignClient          walletFeignClient;

    @InjectMocks private ShizhuangService shizhuangService;

    private static PlayerShizhuang entity(Long roleId, int shizhuangId, int level, boolean activated, boolean wearing) {
        return PlayerShizhuang.builder()
                .roleId(roleId)
                .shizhuangId(shizhuangId)
                .level(level)
                .star(1)
                .activated(activated)
                .wearing(wearing)
                .build();
    }

    // =========================================================
    // listByRole()
    // =========================================================
    @Nested
    @DisplayName("listByRole()")
    class ListByRole {

        @Test
        @DisplayName("TC-SHIZHUANG-001 [P] Lay danh sach shizhuang – tra ve danh sach day du")
        void listByRole_returnsAll() {
            given(repository.findByRoleId(1L)).willReturn(
                    List.of(entity(1L, 1, 1, true, false),
                            entity(1L, 2, 1, false, false)));

            ShizhuangDTOs.ShizhuangListResp result = shizhuangService.listByRole(1L);

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("TC-SHIZHUANG-002 [P] Nguoi dung chua co shizhuang – tra ve danh sach rong")
        void listByRole_empty_returnsEmpty() {
            given(repository.findByRoleId(2L)).willReturn(List.of());

            ShizhuangDTOs.ShizhuangListResp result = shizhuangService.listByRole(2L);

            assertThat(result.getTotalCount()).isEqualTo(0);
        }
    }

    // =========================================================
    // getInfo()
    // =========================================================
    @Nested
    @DisplayName("getInfo()")
    class GetInfo {

        @Test
        @DisplayName("TC-SHIZHUANG-003 [P] Tim thay shizhuang – tra ve thong tin")
        void getInfo_found_returnsDto() {
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 3, true, true)));

            ShizhuangDTOs.ShizhuangInfo result = shizhuangService.getInfo(1L, 1);

            assertThat(result).isNotNull();
            assertThat(result.getLevel()).isEqualTo(3);
            assertThat(result.getActivated()).isTrue();
        }

        @Test
        @DisplayName("TC-SHIZHUANG-004 [N] Khong tim thay shizhuang – tra ve null")
        void getInfo_notFound_returnsNull() {
            given(repository.findByRoleIdAndShizhuangId(1L, 99)).willReturn(Optional.empty());

            ShizhuangDTOs.ShizhuangInfo result = shizhuangService.getInfo(1L, 99);

            assertThat(result).isNull();
        }
    }

    // =========================================================
    // activate()
    // =========================================================
    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("TC-SHIZHUANG-005 [P] Kich hoat shizhuang moi – tra ve ok=true")
        void activate_newShizhuang_returnsOk() {
            ShizhuangDTOs.ActivateReq req = mock(ShizhuangDTOs.ActivateReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(5);
            given(repository.findByRoleIdAndShizhuangId(1L, 5)).willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ShizhuangDTOs.OperationResp result = shizhuangService.activate(req);

            assertThat(result.getOk()).isTrue();
        }

        @Test
        @DisplayName("TC-SHIZHUANG-006 [P] Kich hoat shizhuang da co – cap nhat va tra ve ok=true")
        void activate_existingShizhuang_updatesAndReturnsOk() {
            ShizhuangDTOs.ActivateReq req = mock(ShizhuangDTOs.ActivateReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 1, false, false)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ShizhuangDTOs.OperationResp result = shizhuangService.activate(req);

            assertThat(result.getOk()).isTrue();
        }
    }

    // =========================================================
    // wear()
    // =========================================================
    @Nested
    @DisplayName("wear()")
    class Wear {

        @Test
        @DisplayName("TC-SHIZHUANG-007 [N] Shizhuang khong ton tai – tra ve ok=false")
        void wear_notFound_returnsFalse() {
            ShizhuangDTOs.WearReq req = mock(ShizhuangDTOs.WearReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(99);
            given(repository.findByRoleIdAndWearingTrue(1L)).willReturn(Optional.empty());
            given(repository.findByRoleIdAndShizhuangId(1L, 99)).willReturn(Optional.empty());

            ShizhuangDTOs.OperationResp result = shizhuangService.wear(req);

            assertThat(result.getOk()).isFalse();
        }

        @Test
        @DisplayName("TC-SHIZHUANG-008 [N] Shizhuang chua duoc kich hoat – tra ve ok=false")
        void wear_notActivated_returnsFalse() {
            ShizhuangDTOs.WearReq req = mock(ShizhuangDTOs.WearReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            given(repository.findByRoleIdAndWearingTrue(1L)).willReturn(Optional.empty());
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 1, false, false)));

            ShizhuangDTOs.OperationResp result = shizhuangService.wear(req);

            assertThat(result.getOk()).isFalse();
            assertThat(result.getMessage()).contains("Shizhuang not activated");
        }

        @Test
        @DisplayName("TC-SHIZHUANG-009 [P] Mac shizhuang thanh cong – tra ve ok=true")
        void wear_activated_returnsOk() {
            ShizhuangDTOs.WearReq req = mock(ShizhuangDTOs.WearReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            given(repository.findByRoleIdAndWearingTrue(1L)).willReturn(Optional.empty());
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 1, true, false)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ShizhuangDTOs.OperationResp result = shizhuangService.wear(req);

            assertThat(result.getOk()).isTrue();
        }
    }

    // =========================================================
    // levelUp()
    // =========================================================
    @Nested
    @DisplayName("levelUp()")
    class LevelUp {

        @Test
        @DisplayName("TC-SHIZHUANG-010 [N] Shizhuang khong ton tai – tra ve ok=false")
        void levelUp_notFound_returnsFalse() {
            ShizhuangDTOs.LevelUpReq req = mock(ShizhuangDTOs.LevelUpReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(99);
            given(repository.findByRoleIdAndShizhuangId(1L, 99)).willReturn(Optional.empty());

            ShizhuangDTOs.OperationResp result = shizhuangService.levelUp(req);

            assertThat(result.getOk()).isFalse();
        }

        @Test
        @DisplayName("TC-SHIZHUANG-011 [N] Shizhuang chua kich hoat – tra ve ok=false")
        void levelUp_notActivated_returnsFalse() {
            ShizhuangDTOs.LevelUpReq req = mock(ShizhuangDTOs.LevelUpReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 1, false, false)));

            ShizhuangDTOs.OperationResp result = shizhuangService.levelUp(req);

            assertThat(result.getOk()).isFalse();
            assertThat(result.getMessage()).contains("not activated");
        }

        @Test
        @DisplayName("TC-SHIZHUANG-012 [N] Tru xu that bai – tra ve ok=false")
        void levelUp_walletFails_returnsFalse() {
            ShizhuangDTOs.LevelUpReq req = mock(ShizhuangDTOs.LevelUpReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            given(repository.findByRoleIdAndShizhuangId(1L, 1))
                    .willReturn(Optional.of(entity(1L, 1, 3, true, false)));
            given(walletFeignClient.deductCurrency(any())).willThrow(new RuntimeException("Insufficient gold"));

            ShizhuangDTOs.OperationResp result = shizhuangService.levelUp(req);

            assertThat(result.getOk()).isFalse();
            assertThat(result.getMessage()).contains("Insufficient gold");
        }

        @Test
        @DisplayName("TC-SHIZHUANG-013 [P] Tang cap thanh cong – tra ve ok=true va level++")
        void levelUp_success_returnsOkAndIncrementsLevel() {
            ShizhuangDTOs.LevelUpReq req = mock(ShizhuangDTOs.LevelUpReq.class);
            given(req.getRoleId()).willReturn("1");
            given(req.getShizhuangId()).willReturn(1);
            PlayerShizhuang shizhuang = entity(1L, 1, 5, true, false);
            given(repository.findByRoleIdAndShizhuangId(1L, 1)).willReturn(Optional.of(shizhuang));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ShizhuangDTOs.OperationResp result = shizhuangService.levelUp(req);

            assertThat(result.getOk()).isTrue();
            assertThat(shizhuang.getLevel()).isEqualTo(6);
        }
    }
}
