package com.SouthMillion.knights_service.repository;
import com.SouthMillion.knights_service.entity.KnightsHandbook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KnightsHandbookRepository extends JpaRepository<KnightsHandbook, Long> {
    Optional<KnightsHandbook> findByRoleId(Long roleId);
}
