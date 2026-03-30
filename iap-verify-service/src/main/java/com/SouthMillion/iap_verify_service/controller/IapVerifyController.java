package com.SouthMillion.iap_verify_service.controller;

import com.SouthMillion.iap_verify_service.entity.Purchase;
import com.SouthMillion.iap_verify_service.entity.RefundRequest;
import com.SouthMillion.iap_verify_service.service.IapVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iap")
@RequiredArgsConstructor
@Slf4j
public class IapVerifyController {
    
    private final IapVerifyService iapVerifyService;
    
    /**
     * Verify purchase receipt
     */
    @PostMapping("/verify")
    public ResponseEntity<Purchase> verifyPurchase(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        String platform = request.get("platform").toString();
        String productId = request.get("productId").toString();
        String purchaseToken = request.get("purchaseToken").toString();
        String packageName = request.getOrDefault("packageName", "").toString();

        Purchase purchase = iapVerifyService.verifyPurchase(userId, platform, productId, purchaseToken, packageName);
        return ResponseEntity.ok(purchase);
    }
    
    /**
     * Consume a verified purchase
     */
    @PostMapping("/consume/{purchaseId}")
    public ResponseEntity<Purchase> consumePurchase(@PathVariable Long purchaseId) {
        Purchase purchase = iapVerifyService.consumePurchase(purchaseId);
        return ResponseEntity.ok(purchase);
    }
    
    /**
     * Get user's purchase history
     */
    @GetMapping("/purchases/{userId}")
    public ResponseEntity<List<Purchase>> getUserPurchases(@PathVariable String userId) {
        List<Purchase> purchases = iapVerifyService.getUserPurchases(userId);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Get unconsumed purchases for user
     */
    @GetMapping("/purchases/{userId}/unconsumed")
    public ResponseEntity<List<Purchase>> getUnconsumedPurchases(@PathVariable String userId) {
        List<Purchase> purchases = iapVerifyService.getUnconsumedPurchases(userId);
        return ResponseEntity.ok(purchases);
    }
    
    /**
     * Create refund request
     */
    @PostMapping("/refund/request")
    public ResponseEntity<RefundRequest> createRefundRequest(@RequestBody Map<String, Object> request) {
        Long purchaseId = Long.parseLong(request.get("purchaseId").toString());
        String userId = request.get("userId").toString();
        String reason = request.get("reason").toString();

        RefundRequest refundRequest = iapVerifyService.createRefundRequest(purchaseId, userId, reason);
        return ResponseEntity.ok(refundRequest);
    }
    
    /**
     * Process refund request (GM only)
     */
    @PutMapping("/refund/{refundId}/process")
    public ResponseEntity<RefundRequest> processRefundRequest(
            @PathVariable Long refundId,
            @RequestBody Map<String, Object> request) {
        
        Long gmId = Long.parseLong(request.get("gmId").toString());
        String status = request.get("status").toString();
        String notes = request.getOrDefault("notes", "").toString();
        
        RefundRequest refundRequest = iapVerifyService.processRefundRequest(refundId, gmId, status, notes);
        return ResponseEntity.ok(refundRequest);
    }
    
    /**
     * Get pending refund requests (GM only)
     */
    @GetMapping("/refund/pending")
    public ResponseEntity<List<RefundRequest>> getPendingRefunds() {
        List<RefundRequest> refunds = iapVerifyService.getPendingRefunds();
        return ResponseEntity.ok(refunds);
    }
    
    /**
     * Get suspicious purchases for review (GM only)
     */
    @GetMapping("/suspicious")
    public ResponseEntity<List<Purchase>> getSuspiciousPurchases(
            @RequestParam(defaultValue = "50") int threshold) {
        
        List<Purchase> purchases = iapVerifyService.getSuspiciousPurchases(threshold);
        return ResponseEntity.ok(purchases);
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "iap-verify-service"
        ));
    }
}
