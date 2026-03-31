package com.SouthMillion.pagoda_service.repository;

import com.SouthMillion.pagoda_service.entity.GuMoMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GuMoMetaRepository extends JpaRepository<GuMoMeta, Long> {
    Optional<GuMoMeta> findByRoleId(Long roleId);
}
