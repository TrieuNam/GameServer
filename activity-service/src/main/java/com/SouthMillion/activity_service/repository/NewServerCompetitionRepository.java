package com.SouthMillion.activity_service.repository;

// Refer to ADR-001 for architecture decisions

import com.SouthMillion.activity_service.entity.NewServerCompetition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewServerCompetitionRepository extends JpaRepository<NewServerCompetition, Long> {
    // Custom query to find by role ID
    NewServerCompetition findByRoleId(Long roleId);
}