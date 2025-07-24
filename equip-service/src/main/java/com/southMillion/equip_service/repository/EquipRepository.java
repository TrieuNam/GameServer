package com.southMillion.equip_service.repository;

import com.southMillion.equip_service.entity.EquipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipRepository extends JpaRepository<EquipEntity, Long> {
    Optional<EquipEntity> findByUserIdAndEquipType(String userId, int equipType);

    List<EquipEntity> findByUserId(String userId);
}