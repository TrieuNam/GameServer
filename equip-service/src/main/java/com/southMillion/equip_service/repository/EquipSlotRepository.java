package com.SouthMillion.equip_service.repository;

import com.SouthMillion.equip_service.entity.EquipSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipSlotRepository extends JpaRepository<EquipSlotEntity, Long> {
    List<EquipSlotEntity> findByRoleId(Long roleId);
    Optional<EquipSlotEntity> findByRoleIdAndEquipType(Long roleId, int equipType);
}