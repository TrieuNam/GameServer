package com.SouthMillion.iap_verify_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.iap_verify_service.entity.Purchase;
import com.SouthMillion.iap_verify_service.entity.RefundRequest;
import com.SouthMillion.iap_verify_service.repository.PurchaseRepository;
import com.SouthMillion.iap_verify_service.repository.RefundRequestRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IapVerifyService {
    
    private final PurchaseRepository purchaseRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${iap.apple.sandbox-url}")
    private String appleSandboxUrl;
    
    @Value("${iap.apple.production-url}")
    private String appleProductionUrl;
    
    @Value("${iap.apple.shared-secret}")
    private String appleSharedSecret;
    
    @Value("${iap.apple.use-sandbox}")
    private boolean useAppleSandbox;
    
    @Value("${iap.verification.timeout}")
    private int verificationTimeout;

    @Value("${iap.verification.cache-duration}")
    private int cacheDuration;

    @Value("${iap.google.credentials-path:}")
    private String googleCredentialsPath;

    @Value("${iap.google.application-name:GameServer}")
    private String googleApplicationName;
    
    /**
     * Verify purchase from Google Play or Apple App Store
     */
    @Transactional
    public Purchase verifyPurchase(String userId, String platform, String productId, String purchaseToken, String packageName) {
        log.info("Verifying purchase: userId={}, platform={}, productId={}, token={}", userId, platform, productId, purchaseToken);
        
        // Check cache first
        String cacheKey = "purchase:verified:" + purchaseToken;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Purchase verification found in cache: {}", purchaseToken);
            return purchaseRepository.findByPurchaseToken(purchaseToken).orElse(null);
        }
        
        // Check if already verified
        Purchase existingPurchase = purchaseRepository.findByPurchaseToken(purchaseToken).orElse(null);
        if (existingPurchase != null && "VERIFIED".equals(existingPurchase.getVerificationStatus())) {
            log.info("Purchase already verified: {}", purchaseToken);
            redisTemplate.opsForValue().set(cacheKey, "true", cacheDuration, TimeUnit.SECONDS);
            return existingPurchase;
        }
        
        // Create or update purchase record
        Purchase purchase = existingPurchase != null ? existingPurchase : new Purchase();
        purchase.setUserId(userId);
        purchase.setPlatform(platform.toUpperCase());
        purchase.setProductId(productId);
        purchase.setPurchaseToken(purchaseToken);
        purchase.setPackageName(packageName);
        purchase.setVerificationStatus("PENDING");
        purchase.setConsumptionStatus("UNCONSUMED");
        
        try {
            // Verify with platform API
            Map<String, Object> verificationResult;
            if ("GOOGLE".equalsIgnoreCase(platform)) {
                verificationResult = verifyGooglePlayPurchase(productId, purchaseToken, packageName);
            } else if ("APPLE".equalsIgnoreCase(platform)) {
                verificationResult = verifyAppleStorePurchase(purchaseToken);
            } else {
                throw new IllegalArgumentException("Unsupported platform: " + platform);
            }
            
            // Process verification result
            boolean isValid = (boolean) verificationResult.get("valid");
            purchase.setRawResponse(objectMapper.writeValueAsString(verificationResult));
            
            if (isValid) {
                purchase.setVerificationStatus("VERIFIED");
                purchase.setPurchaseState("PURCHASED");
                purchase.setVerificationTime(LocalDateTime.now());
                
                // Extract additional data from response
                if (verificationResult.containsKey("orderId")) {
                    purchase.setOrderId((String) verificationResult.get("orderId"));
                }
                if (verificationResult.containsKey("purchaseTimeMillis")) {
                    long millis = Long.parseLong(verificationResult.get("purchaseTimeMillis").toString());
                    purchase.setPurchaseTime(LocalDateTime.now().minusSeconds((System.currentTimeMillis() - millis) / 1000));
                }
                
                // Calculate fraud score
                int fraudScore = calculateFraudScore(userId, purchase);
                purchase.setFraudScore(fraudScore);
                
                // Cache result
                redisTemplate.opsForValue().set(cacheKey, "true", cacheDuration, TimeUnit.SECONDS);
                
                log.info("Purchase verified successfully: {}", purchaseToken);
            } else {
                purchase.setVerificationStatus("FAILED");
                purchase.setErrorMessage("Verification failed: Invalid receipt");
                log.warn("Purchase verification failed: {}", purchaseToken);
            }
            
        } catch (Exception e) {
            log.error("Error verifying purchase: {}", e.getMessage(), e);
            purchase.setVerificationStatus("FAILED");
            purchase.setErrorMessage(e.getMessage());
        }
        
        return purchaseRepository.save(purchase);
    }
    
    /**
     * Verify Google Play purchase via Google Play Developer API
     */
    private Map<String, Object> verifyGooglePlayPurchase(String productId, String purchaseToken, String packageName) {
        log.info("Verifying Google Play purchase: {}", purchaseToken);

        if (googleCredentialsPath == null || googleCredentialsPath.isEmpty()) {
            log.error("Google Play credentials not configured. Set iap.google.credentials-path");
            return Map.of("valid", false, "error", "Google Play credentials not configured");
        }

        try (InputStream credStream = new FileInputStream(googleCredentialsPath)) {
            ServiceAccountCredentials credentials = (ServiceAccountCredentials)
                ServiceAccountCredentials.fromStream(credStream)
                    .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));

            HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

            AndroidPublisher publisher = new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName(googleApplicationName)
                .build();

            ProductPurchase purchase = publisher.purchases().products()
                .get(packageName, productId, purchaseToken)
                .execute();

            boolean valid = purchase.getPurchaseState() != null && purchase.getPurchaseState() == 0;

            Map<String, Object> result = new HashMap<>();
            result.put("valid", valid);
            result.put("productId", productId);
            result.put("orderId", purchase.getOrderId() != null ? purchase.getOrderId() : "");
            result.put("purchaseTimeMillis", purchase.getPurchaseTimeMillis() != null ? purchase.getPurchaseTimeMillis() : 0L);
            result.put("purchaseState", purchase.getPurchaseState() != null ? purchase.getPurchaseState() : -1);
            result.put("consumptionState", purchase.getConsumptionState() != null ? purchase.getConsumptionState() : 0);
            result.put("acknowledged", purchase.getAcknowledgementState() != null && purchase.getAcknowledgementState() == 1);

            log.info("Google Play verification result: valid={}, orderId={}", valid, result.get("orderId"));
            return result;

        } catch (Exception e) {
            log.error("Google Play verification error: {}", e.getMessage(), e);
            return Map.of("valid", false, "error", e.getMessage());
        }
    }
    
    /**
     * Verify Apple App Store purchase
     */
    private Map<String, Object> verifyAppleStorePurchase(String receiptData) {
        log.info("Verifying Apple App Store purchase");
        
        try {
            String url = useAppleSandbox ? appleSandboxUrl : appleProductionUrl;
            
            WebClient webClient = webClientBuilder.build();
            
            Map<String, String> requestBody = Map.of(
                "receipt-data", receiptData,
                "password", appleSharedSecret,
                "exclude-old-transactions", "true"
            );
            
            String response = webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(verificationTimeout))
                .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            int status = jsonResponse.get("status").asInt();
            
            if (status == 0) {
                // Valid receipt
                JsonNode receipt = jsonResponse.get("receipt");
                return Map.of(
                    "valid", true,
                    "status", status,
                    "bundleId", receipt.get("bundle_id").asText(),
                    "productId", receipt.get("in_app").get(0).get("product_id").asText(),
                    "transactionId", receipt.get("in_app").get(0).get("transaction_id").asText(),
                    "purchaseDate", receipt.get("in_app").get(0).get("purchase_date_ms").asText()
                );
            } else if (status == 21007) {
                // Sandbox receipt sent to production - retry with sandbox
                log.warn("Sandbox receipt detected, retrying with sandbox URL");
                // Recursive call with sandbox
                return verifyAppleStorePurchase(receiptData);
            } else {
                return Map.of("valid", false, "status", status, "error", "Invalid receipt");
            }
            
        } catch (Exception e) {
            log.error("Apple Store verification error: {}", e.getMessage(), e);
            return Map.of("valid", false, "error", e.getMessage());
        }
    }
    
    /**
     * Calculate fraud score based on purchase patterns
     */
    private int calculateFraudScore(String userId, Purchase purchase) {
        int score = 0;
        
        try {
            // Check purchase frequency
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Long recentPurchases = purchaseRepository.countPurchasesByUserIdSince(userId, oneDayAgo);
            
            if (recentPurchases > 10) {
                score += 30; // Suspicious: too many purchases in 24h
            } else if (recentPurchases > 5) {
                score += 15;
            }
            
            // Check for duplicate tokens (should be unique)
            Long duplicateCount = purchaseRepository.findByPurchaseToken(purchase.getPurchaseToken())
                .map(p -> 1L).orElse(0L);
            if (duplicateCount > 0) {
                score += 50; // Very suspicious: duplicate token
            }
            
            // Check if user has refund history
            List<RefundRequest> refunds = refundRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (refunds.size() > 3) {
                score += 20; // Suspicious: multiple refunds
            }
            
        } catch (Exception e) {
            log.error("Error calculating fraud score: {}", e.getMessage(), e);
        }
        
        return Math.min(score, 100); // Cap at 100
    }
    
    /**
     * Mark purchase as consumed
     */
    @Transactional
    public Purchase consumePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
        
        if (!"VERIFIED".equals(purchase.getVerificationStatus())) {
            throw new IllegalStateException("Cannot consume unverified purchase");
        }
        
        if ("CONSUMED".equals(purchase.getConsumptionStatus())) {
            throw new IllegalStateException("Purchase already consumed");
        }
        
        purchase.setConsumptionStatus("CONSUMED");
        purchase.setConsumedTime(LocalDateTime.now());
        
        log.info("Purchase consumed: purchaseId={}, userId={}", purchaseId, purchase.getUserId());
        
        return purchaseRepository.save(purchase);
    }
    
    /**
     * Get user's purchase history
     */
    public List<Purchase> getUserPurchases(String userId) {
        return purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unconsumed purchases for user
     */
    public List<Purchase> getUnconsumedPurchases(String userId) {
        return purchaseRepository.findUnconsumedPurchasesByUserId(userId);
    }

    /**
     * Create refund request
     */
    @Transactional
    public RefundRequest createRefundRequest(Long purchaseId, String userId, String reason) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));
        
        if (!purchase.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Purchase does not belong to user");
        }
        
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setPurchaseId(purchaseId);
        refundRequest.setUserId(userId);
        refundRequest.setReason(reason);
        refundRequest.setStatus("PENDING");
        refundRequest.setRefundAmount(purchase.getAmount());
        
        log.info("Refund request created: purchaseId={}, userId={}", purchaseId, userId);
        
        return refundRequestRepository.save(refundRequest);
    }
    
    /**
     * Process refund request (GM action)
     */
    @Transactional
    public RefundRequest processRefundRequest(Long refundId, Long gmId, String status, String notes) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundId)
            .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + refundId));
        
        refundRequest.setStatus(status.toUpperCase());
        refundRequest.setProcessedBy(gmId);
        refundRequest.setProcessedAt(LocalDateTime.now());
        refundRequest.setResolutionNotes(notes);
        
        if ("APPROVED".equals(status.toUpperCase())) {
            // Update purchase status
            Purchase purchase = purchaseRepository.findById(refundRequest.getPurchaseId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
            purchase.setPurchaseState("REFUNDED");
        }
        
        log.info("Refund request processed: refundId={}, status={}, gmId={}", refundId, status, gmId);
        
        return refundRequestRepository.save(refundRequest);
    }
    
    /**
     * Get pending refund requests
     */
    public List<RefundRequest> getPendingRefunds() {
        return refundRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }
    
    /**
     * Get suspicious purchases for review
     */
    public List<Purchase> getSuspiciousPurchases(int threshold) {
        return purchaseRepository.findSuspiciousPurchases(threshold);
    }
}
