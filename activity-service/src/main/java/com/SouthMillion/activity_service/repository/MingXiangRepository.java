package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.MingXiang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MingXiangRepository extends JpaRepository<MingXiang, Long> {
    Optional<MingXiang> findByRoleId(Long roleId);
}
