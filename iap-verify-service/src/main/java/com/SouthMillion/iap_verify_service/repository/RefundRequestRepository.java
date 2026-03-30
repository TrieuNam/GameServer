package com.SouthMillion.iap_verify_service.repository;

import com.SouthMillion.iap_verify_service.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    
    List<RefundRequest> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<RefundRequest> findByStatusOrderByCreatedAtDesc(String status);
    
    List<RefundRequest> findByPurchaseId(Long purchaseId);
}
