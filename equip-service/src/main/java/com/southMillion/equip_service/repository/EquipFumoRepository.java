package com.southMillion.equip_service.repository;

import com.southMillion.equip_service.entity.EquipFumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipFumoRepository extends JpaRepository<EquipFumoEntity, Long> {
    List<EquipFumoEntity> findByRoleId(String roleId);
    Optional<EquipFumoEntity> findByRoleIdAndEquipType(String roleId, int equipType);
}