package com.SouthMillion.equip_service.repository;

import com.SouthMillion.equip_service.entity.EquipFumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipFumoRepository extends JpaRepository<EquipFumoEntity, Long> {
    List<EquipFumoEntity> findByRoleId(Long roleId);
    Optional<EquipFumoEntity> findByRoleIdAndEquipType(Long roleId, int equipType);
}