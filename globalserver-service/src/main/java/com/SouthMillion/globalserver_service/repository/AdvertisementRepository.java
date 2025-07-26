package com.SouthMillion.globalserver_service.repository;

import com.SouthMillion.globalserver_service.entity.AdvertisementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdvertisementRepository extends JpaRepository<AdvertisementEntity, Long> {
    List<AdvertisementEntity> findAllByUserId(Long userId);
    Optional<AdvertisementEntity> findByUserIdAndSeq(Long userId, Integer seq);
}