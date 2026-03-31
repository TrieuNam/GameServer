package com.SouthMillion.iap_verify_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "platform", nullable = false, length = 20)
    private String platform; // GOOGLE, APPLE
    
    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;
    
    @Column(name = "purchase_token", nullable = false, length = 500, unique = true)
    private String purchaseToken;
    
    @Column(name = "order_id", length = 100)
    private String orderId;
    
    @Column(name = "package_name", length = 100)
    private String packageName;
    
    @Column(name = "purchase_time")
    private LocalDateTime purchaseTime;
    
    @Column(name = "purchase_state", length = 20)
    private String purchaseState; // PURCHASED, PENDING, REFUNDED, CANCELLED
    
    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus; // PENDING, VERIFIED, FAILED, FRAUD
    
    @Column(name = "verification_time")
    private LocalDateTime verificationTime;
    
    @Column(name = "consumption_status", nullable = false, length = 20)
    private String consumptionStatus; // UNCONSUMED, CONSUMED, EXPIRED
    
    @Column(name = "consumed_time")
    private LocalDateTime consumedTime;
    
    @Column(name = "amount")
    private Long amount; // Price in cents
    
    @Column(name = "currency", length = 10)
    private String currency;
    
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "fraud_score")
    private Integer fraudScore; // 0-100
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
