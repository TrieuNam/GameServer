package com.SouthMillion.gem_service.repository;
import com.SouthMillion.gem_service.entity.GemData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface GemDataRepository extends JpaRepository<GemData, Long> {
    List<GemData> findByRoleId(Long roleId);
    Optional<GemData> findByRoleIdAndGemId(Long roleId, Integer gemId);
    List<GemData> findByRoleIdAndIsInlaid(Long roleId, Boolean isInlaid);
}
