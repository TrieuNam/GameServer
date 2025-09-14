package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.AdRewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AdRewardClaimRepository extends JpaRepository<AdRewardClaim, String> {
    Optional<AdRewardClaim> findByUserIdAndSeqAndClaimDay(String userId, int seq, LocalDate claimDay);
}