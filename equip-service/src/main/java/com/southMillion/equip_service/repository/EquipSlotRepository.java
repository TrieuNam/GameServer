package com.southMillion.equip_service.repository;

import com.southMillion.equip_service.entity.EquipSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipSlotRepository extends JpaRepository<EquipSlotEntity, Long> {
    List<EquipSlotEntity> findByRoleId(String roleId);
    Optional<EquipSlotEntity> findByRoleIdAndEquipType(String roleId, int equipType);
}