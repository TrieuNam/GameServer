package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.AdvertisementEquity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdvertisementEquityRepository extends JpaRepository<AdvertisementEquity, Long> {
    Optional<AdvertisementEquity> findByRoleId(Long roleId);
}
