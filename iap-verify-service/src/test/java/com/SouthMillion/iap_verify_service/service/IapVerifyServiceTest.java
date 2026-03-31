package com.SouthMillion.iap_verify_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.iap_verify_service.entity.Purchase;
import com.SouthMillion.iap_verify_service.entity.RefundRequest;
import com.SouthMillion.iap_verify_service.repository.PurchaseRepository;
import com.SouthMillion.iap_verify_service.repository.RefundRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("IapVerifyService Tests")
class IapVerifyServiceTest {

    @Mock private PurchaseRepository      purchaseRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private WebClient.Builder       webClientBuilder;
    @Mock private ObjectMapper            objectMapper;

    @InjectMocks private IapVerifyService iapVerifyService;

    // =========================================================
    // consumePurchase
    // =========================================================
    @Nested
    @DisplayName("consumePurchase()")
    class ConsumePurchase {

        @Test
        @DisplayName("TC-IAP-001 [P] Tieu thu giao dich hop le – thanh cong")
        void consumePurchase_verified_unconsumed_success() {
            Purchase purchase = new Purchase();
            purchase.setId(1L);
            purchase.setUserId("user-010");
            purchase.setVerificationStatus("VERIFIED");
            purchase.setConsumptionStatus("UNCONSUMED");
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
            given(purchaseRepository.save(any(Purchase.class))).willAnswer(inv -> inv.getArgument(0));

            Purchase result = iapVerifyService.consumePurchase(1L);

            assertThat(result.getConsumptionStatus()).isEqualTo("CONSUMED");
            assertThat(result.getConsumedTime()).isNotNull();
        }

        @Test
        @DisplayName("TC-IAP-002 [N] Giao dich khong ton tai – nem IllegalArgumentException")
        void consumePurchase_notFound_throwsException() {
            given(purchaseRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> iapVerifyService.consumePurchase(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Purchase not found");
        }

        @Test
        @DisplayName("TC-IAP-003 [N] Giao dich chua xac minh – nem IllegalStateException")
        void consumePurchase_notVerified_throwsException() {
            Purchase purchase = new Purchase();
            purchase.setId(2L);
            purchase.setVerificationStatus("PENDING");
            purchase.setConsumptionStatus("UNCONSUMED");
            given(purchaseRepository.findById(2L)).willReturn(Optional.of(purchase));

            assertThatThrownBy(() -> iapVerifyService.consumePurchase(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot consume unverified");
        }

        @Test
        @DisplayName("TC-IAP-004 [N] Giao dich da tieu thu – nem IllegalStateException")
        void consumePurchase_alreadyConsumed_throwsException() {
            Purchase purchase = new Purchase();
            purchase.setId(3L);
            purchase.setVerificationStatus("VERIFIED");
            purchase.setConsumptionStatus("CONSUMED");
            given(purchaseRepository.findById(3L)).willReturn(Optional.of(purchase));

            assertThatThrownBy(() -> iapVerifyService.consumePurchase(3L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already consumed");
        }
    }

    // =========================================================
    // createRefundRequest
    // =========================================================
    @Nested
    @DisplayName("createRefundRequest()")
    class CreateRefundRequest {

        @Test
        @DisplayName("TC-IAP-005 [P] Tao yeu cau hoan tien thanh cong")
        void createRefundRequest_success() {
            Purchase purchase = new Purchase();
            purchase.setId(1L);
            purchase.setUserId("user-010");
            purchase.setAmount(9900L);
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));
            given(refundRequestRepository.save(any(RefundRequest.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            RefundRequest result = iapVerifyService.createRefundRequest(1L, "user-010", "Changed my mind");

            assertThat(result.getPurchaseId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo("user-010");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getRefundAmount()).isEqualTo(9900L);
        }

        @Test
        @DisplayName("TC-IAP-006 [N] Giao dich khong ton tai – nem IllegalArgumentException")
        void createRefundRequest_purchaseNotFound_throwsException() {
            given(purchaseRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> iapVerifyService.createRefundRequest(999L, "user-010", "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Purchase not found");
        }

        @Test
        @DisplayName("TC-IAP-007 [N] Giao dich khong thuoc ve nguoi dung – nem IllegalArgumentException")
        void createRefundRequest_wrongUser_throwsException() {
            Purchase purchase = new Purchase();
            purchase.setId(1L);
            purchase.setUserId("user-010"); // owner is userId=10
            given(purchaseRepository.findById(1L)).willReturn(Optional.of(purchase));

            // userId=99 tries to refund userId=10's purchase
            assertThatThrownBy(() -> iapVerifyService.createRefundRequest(1L, "user-099", "Reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Purchase does not belong to user");
        }
    }

    // =========================================================
    // processRefundRequest
    // =========================================================
    @Nested
    @DisplayName("processRefundRequest()")
    class ProcessRefundRequest {

        @Test
        @DisplayName("TC-IAP-008 [P] Chap thuan hoan tien – cap nhat trang thai APPROVED")
        void processRefundRequest_approved_updatesPurchaseAndRefund() {
            RefundRequest refund = new RefundRequest();
            refund.setId(1L);
            refund.setPurchaseId(5L);
            refund.setStatus("PENDING");
            given(refundRequestRepository.findById(1L)).willReturn(Optional.of(refund));
            given(refundRequestRepository.save(any(RefundRequest.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // For APPROVED, purchaseRepository.findById is called to update purchase state
            Purchase purchase = new Purchase();
            purchase.setId(5L);
            purchase.setPurchaseState("PURCHASED");
            given(purchaseRepository.findById(5L)).willReturn(Optional.of(purchase));

            RefundRequest result = iapVerifyService.processRefundRequest(1L, 99L, "APPROVED", "Valid reason");

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getProcessedBy()).isEqualTo(99L);
            assertThat(result.getProcessedAt()).isNotNull();
            // Purchase state updated
            assertThat(purchase.getPurchaseState()).isEqualTo("REFUNDED");
        }

        @Test
        @DisplayName("TC-IAP-009 [P] Tu choi hoan tien – cap nhat trang thai REJECTED")
        void processRefundRequest_rejected_updatesStatus() {
            RefundRequest refund = new RefundRequest();
            refund.setId(2L);
            refund.setPurchaseId(6L);
            refund.setStatus("PENDING");
            given(refundRequestRepository.findById(2L)).willReturn(Optional.of(refund));
            given(refundRequestRepository.save(any(RefundRequest.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            RefundRequest result = iapVerifyService.processRefundRequest(2L, 99L, "REJECTED", "No valid reason");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            then(purchaseRepository).shouldHaveNoInteractions(); // no purchase update for REJECTED
        }

        @Test
        @DisplayName("TC-IAP-010 [N] Yeu cau hoan tien khong ton tai – nem IllegalArgumentException")
        void processRefundRequest_notFound_throwsException() {
            given(refundRequestRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> iapVerifyService.processRefundRequest(999L, 1L, "APPROVED", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Refund request not found");
        }
    }

    // =========================================================
    // getUserPurchases / getUnconsumedPurchases / getSuspiciousPurchases
    // =========================================================
    @Nested
    @DisplayName("getUserPurchases() / getUnconsumedPurchases()")
    class Queries {

        @Test
        @DisplayName("TC-IAP-011 [P] Lay lich su giao dich cua nguoi dung")
        void getUserPurchases_returnsList() {
            Purchase p = new Purchase();
            p.setUserId("user-010");
            given(purchaseRepository.findByUserIdOrderByCreatedAtDesc("user-010")).willReturn(List.of(p));

            List<Purchase> result = iapVerifyService.getUserPurchases("user-010");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-IAP-012 [P] Lay giao dich chua tieu thu")
        void getUnconsumedPurchases_returnsList() {
            Purchase p = new Purchase();
            p.setConsumptionStatus("UNCONSUMED");
            given(purchaseRepository.findUnconsumedPurchasesByUserId("user-010")).willReturn(List.of(p));

            List<Purchase> result = iapVerifyService.getUnconsumedPurchases("user-010");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-IAP-013 [P] Lay giao dich kha nghi (fraud score cao)")
        void getSuspiciousPurchases_returnsList() {
            Purchase p = new Purchase();
            p.setFraudScore(80);
            given(purchaseRepository.findSuspiciousPurchases(70)).willReturn(List.of(p));

            List<Purchase> result = iapVerifyService.getSuspiciousPurchases(70);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFraudScore()).isEqualTo(80);
        }

        @Test
        @DisplayName("TC-IAP-014 [P] Lay pending refunds")
        void getPendingRefunds_returnsList() {
            RefundRequest refund = new RefundRequest();
            refund.setStatus("PENDING");
            given(refundRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                    .willReturn(List.of(refund));

            List<RefundRequest> result = iapVerifyService.getPendingRefunds();

            assertThat(result).hasSize(1);
        }
    }
}
