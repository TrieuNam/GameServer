package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetRemainsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRemainsRepository extends JpaRepository<PetRemainsEntity, Long> {
    List<PetRemainsEntity> findByUserId(Long userId);
    List<PetRemainsEntity> findByUserIdAndSeq(Long userId, Integer seq);
    // Thêm method nếu cần filter theo các trường khác
}