package com.SouthMillion.gm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.gm.dto.BroadcastRequest;
import com.SouthMillion.gm.dto.CurrencyOperationRequest;
import com.SouthMillion.gm.dto.GiveItemRequest;
import com.SouthMillion.gm.entity.GMActionLog;
import com.SouthMillion.gm.feign.BagFeignClient;
import com.SouthMillion.gm.feign.RoleFeignClient;
import com.SouthMillion.gm.feign.UserFeignClient;
import com.SouthMillion.gm.feign.WalletFeignClient;
import com.SouthMillion.gm.repository.GMActionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GMService Tests")
class GMServiceTest {

    @Mock private GMActionLogRepository actionLogRepository;
    @Mock private BagFeignClient bagFeignClient;
    @Mock private WalletFeignClient walletFeignClient;
    @Mock private RoleFeignClient roleFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private GMService gmService;

    private static final Long GM_ID   = 1L;
    private static final String GM_NAME = "admin";
    private static final String IP     = "127.0.0.1";

    // =========================================================
    // giveItems
    // =========================================================
    @Nested
    @DisplayName("giveItems()")
    class GiveItems {

        @Test
        @DisplayName("TC-GM-001 [P] Them vat pham thanh cong – tra ve ket qua feign")
        void giveItems_success_returnsResult() {
            GiveItemRequest req = new GiveItemRequest("player-1", 100L, 5, "Test reward");
            Map<String, Object> expected = Map.of("success", true);
            given(bagFeignClient.addItems(eq("player-1"), any())).willReturn(expected);

            Map<String, Object> result = gmService.giveItems(GM_ID, GM_NAME, req, IP);

            assertThat(result).isEqualTo(expected);
            then(bagFeignClient).should().addItems(eq("player-1"), any());
            then(actionLogRepository).should().save(any(GMActionLog.class));
        }

        @Test
        @DisplayName("TC-GM-002 [N] Bag feign loi – nem RuntimeException, ghi log FAILED")
        void giveItems_feignFails_throwsRuntimeException() {
            GiveItemRequest req = new GiveItemRequest("player-1", 100L, 5, "Test reward");
            given(bagFeignClient.addItems(any(), any()))
                    .willThrow(new RuntimeException("Network error"));

            assertThatThrownBy(() -> gmService.giveItems(GM_ID, GM_NAME, req, IP))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to give items");
            then(actionLogRepository).should().save(any(GMActionLog.class));
        }
    }

    // =========================================================
    // addCurrency / deductCurrency
    // =========================================================
    @Nested
    @DisplayName("addCurrency() / deductCurrency()")
    class Currency {

        @Test
        @DisplayName("TC-GM-003 [P] Them currency thanh cong")
        void addCurrency_success() {
            CurrencyOperationRequest req = new CurrencyOperationRequest("player-1", "GOLD", 1000L, "Gift");
            Map<String, Object> expected = Map.of("balance", 5000);
            given(walletFeignClient.addCurrency(eq("player-1"), any())).willReturn(expected);

            Map<String, Object> result = gmService.addCurrency(GM_ID, GM_NAME, req, IP);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("TC-GM-004 [N] Wallet feign loi khi them currency – nem RuntimeException")
        void addCurrency_feignFails_throwsException() {
            CurrencyOperationRequest req = new CurrencyOperationRequest("player-1", "GOLD", 1000L, "Gift");
            given(walletFeignClient.addCurrency(any(), any()))
                    .willThrow(new RuntimeException("Wallet error"));

            assertThatThrownBy(() -> gmService.addCurrency(GM_ID, GM_NAME, req, IP))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to add currency");
        }

        @Test
        @DisplayName("TC-GM-005 [P] Tru currency thanh cong")
        void deductCurrency_success() {
            CurrencyOperationRequest req = new CurrencyOperationRequest("player-1", "GOLD", 200L, "Penalty");
            Map<String, Object> expected = Map.of("balance", 300);
            given(walletFeignClient.deductCurrency(eq("player-1"), any())).willReturn(expected);

            Map<String, Object> result = gmService.deductCurrency(GM_ID, GM_NAME, req, IP);

            assertThat(result).isEqualTo(expected);
        }
    }

    // =========================================================
    // banUser / unbanUser
    // =========================================================
    @Nested
    @DisplayName("banUser() / unbanUser()")
    class BanUnban {

        @Test
        @DisplayName("TC-GM-006 [P] Ban nguoi dung thanh cong")
        void banUser_success() {
            Map<String, Object> expected = Map.of("banned", true);
            given(userFeignClient.banUser(eq(42L), any())).willReturn(expected);

            Map<String, Object> result = gmService.banUser(GM_ID, GM_NAME, 42L, "Cheating", 7, IP);

            assertThat(result).isEqualTo(expected);
            then(actionLogRepository).should().save(any(GMActionLog.class));
        }

        @Test
        @DisplayName("TC-GM-007 [P] Unban nguoi dung thanh cong")
        void unbanUser_success() {
            Map<String, Object> expected = Map.of("unbanned", true);
            given(userFeignClient.unbanUser(eq(42L))).willReturn(expected);

            Map<String, Object> result = gmService.unbanUser(GM_ID, GM_NAME, 42L, "Appeal granted", IP);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("TC-GM-008 [N] User feign loi khi ban – nem RuntimeException")
        void banUser_feignFails_throwsException() {
            given(userFeignClient.banUser(any(), any()))
                    .willThrow(new RuntimeException("User service unavailable"));

            assertThatThrownBy(() -> gmService.banUser(GM_ID, GM_NAME, 99L, "Spam", 1, IP))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to ban user");
        }
    }

    // =========================================================
    // broadcastMessage
    // =========================================================
    @Nested
    @DisplayName("broadcastMessage()")
    class BroadcastMessage {

        @Test
        @DisplayName("TC-GM-009 [P] Broadcast thanh cong – tra ve message xac nhan")
        void broadcastMessage_success() {
            BroadcastRequest req = new BroadcastRequest("Server restarts in 5 min", "SYSTEM", 300);

            String result = gmService.broadcastMessage(GM_ID, GM_NAME, req, IP);

            assertThat(result).isEqualTo("Message broadcast successfully");
            then(actionLogRepository).should().save(any(GMActionLog.class));
        }
    }

    // =========================================================
    // getPlayerDetails
    // =========================================================
    @Nested
    @DisplayName("getPlayerDetails()")
    class PlayerDetails {

        @Test
        @DisplayName("TC-GM-010 [P] Lay thong tin nguoi choi – gop inventory va wallet")
        void getPlayerDetails_returnsCombinedInfo() {
            Map<String, Object> inventory = Map.of("items", List.of());
            Map<String, Object> wallet    = Map.of("gold", 500L);
            given(bagFeignClient.getInventory("player-1")).willReturn(inventory);
            given(walletFeignClient.getWallet("player-1")).willReturn(wallet);

            Map<String, Object> result = gmService.getPlayerDetails("player-1");

            assertThat(result).containsKey("playerId")
                              .containsKey("inventory")
                              .containsKey("wallet");
            assertThat(result.get("playerId")).isEqualTo("player-1");
        }

        @Test
        @DisplayName("TC-GM-011 [N] Feign loi khi lay chi tiet – nem RuntimeException")
        void getPlayerDetails_feignFails_throwsException() {
            given(bagFeignClient.getInventory("player-1"))
                    .willThrow(new RuntimeException("Bag service down"));

            assertThatThrownBy(() -> gmService.getPlayerDetails("player-1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to get player details");
        }
    }

    // =========================================================
    // getUserInfo / getUserRoles
    // =========================================================
    @Nested
    @DisplayName("getUserInfo() / getUserRoles()")
    class UserInfo {

        @Test
        @DisplayName("TC-GM-012 [P] Lay thong tin user theo userId")
        void getUserInfo_returnsInfo() {
            Map<String, Object> info = Map.of("id", 10L, "username", "alice");
            given(userFeignClient.getUserInfo(10L)).willReturn(info);

            Map<String, Object> result = gmService.getUserInfo(10L);

            assertThat(result).isEqualTo(info);
        }

        @Test
        @DisplayName("TC-GM-013 [P] Lay roles cua user")
        void getUserRoles_returnsRoles() {
            List<Map<String, Object>> rolesList = List.of();
            given(roleFeignClient.getRolesByUserId("10")).willReturn(rolesList);

            Map<String, Object> result = gmService.getUserRoles(10L);

            assertThat(result).containsEntry("roles", rolesList)
                              .containsEntry("count", 0);
        }
    }

    // =========================================================
    // getGMLogs / getRecentLogs
    // =========================================================
    @Nested
    @DisplayName("getGMLogs() / getRecentLogs()")
    class Logs {

        @Test
        @DisplayName("TC-GM-014 [P] Lay logs theo GM id – tra ve Page rong")
        void getGMLogs_returnsPage() {
            given(actionLogRepository.findByGmIdOrderByTimestampDesc(eq(GM_ID), any(Pageable.class)))
                    .willReturn(Page.empty());

            Page<GMActionLog> result = gmService.getGMLogs(GM_ID, Pageable.unpaged());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-GM-015 [P] Lay 100 logs moi nhat")
        void getRecentLogs_returnsList() {
            GMActionLog log1 = new GMActionLog();
            log1.setId(1L);
            log1.setGmUsername("admin");
            given(actionLogRepository.findTop100ByOrderByTimestampDesc()).willReturn(List.of(log1));

            List<GMActionLog> result = gmService.getRecentLogs();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGmUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("TC-GM-016 [P] Lay logs theo target player")
        void getPlayerActionLogs_returnsPage() {
            given(actionLogRepository.findByTargetPlayerIdOrderByTimestampDesc(
                    eq("player-1"), any(Pageable.class))).willReturn(Page.empty());

            Page<GMActionLog> result = gmService.getPlayerActionLogs("player-1", Pageable.unpaged());

            assertThat(result).isEmpty();
        }
    }
}
