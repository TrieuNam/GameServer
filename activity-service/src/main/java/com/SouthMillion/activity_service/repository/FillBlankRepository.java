package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FillBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FillBlankRepository extends JpaRepository<FillBlank, Long> {
    Optional<FillBlank> findByRoleId(Long roleId);
}
