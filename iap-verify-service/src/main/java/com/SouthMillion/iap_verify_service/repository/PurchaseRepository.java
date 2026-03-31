package com.SouthMillion.iap_verify_service.repository;

import com.SouthMillion.iap_verify_service.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    
    Optional<Purchase> findByPurchaseToken(String purchaseToken);
    
    List<Purchase> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Purchase> findByVerificationStatusOrderByCreatedAtDesc(String verificationStatus);

    List<Purchase> findByConsumptionStatusOrderByCreatedAtDesc(String consumptionStatus);

    @Query("SELECT p FROM Purchase p WHERE p.userId = :userId AND p.purchaseState = 'PURCHASED' AND p.verificationStatus = 'VERIFIED' AND p.consumptionStatus = 'UNCONSUMED'")
    List<Purchase> findUnconsumedPurchasesByUserId(String userId);

    @Query("SELECT p FROM Purchase p WHERE p.fraudScore >= :threshold")
    List<Purchase> findSuspiciousPurchases(Integer threshold);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.userId = :userId AND p.createdAt >= :since")
    Long countPurchasesByUserIdSince(String userId, LocalDateTime since);
}
