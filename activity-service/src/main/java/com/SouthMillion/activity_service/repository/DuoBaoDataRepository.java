package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.DuoBaoData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DuoBaoDataRepository extends JpaRepository<DuoBaoData, Long> {

    Optional<DuoBaoData> findByRoleIdAndDuoBaoType(Long roleId, Integer duoBaoType);

    List<DuoBaoData> findAllByRoleId(Long roleId);
}
