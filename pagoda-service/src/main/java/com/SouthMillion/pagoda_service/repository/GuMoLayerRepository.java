package com.SouthMillion.pagoda_service.repository;

import com.SouthMillion.pagoda_service.entity.GuMoLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuMoLayerRepository extends JpaRepository<GuMoLayer, Long> {
    List<GuMoLayer> findByRoleId(Long roleId);
    Optional<GuMoLayer> findByRoleIdAndLayerId(Long roleId, Integer layerId);
}
