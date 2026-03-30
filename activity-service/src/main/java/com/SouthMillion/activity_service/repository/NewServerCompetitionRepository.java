package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.NewServerCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewServerCompetitionRepository extends JpaRepository<NewServerCompetition, Long> {
    Optional<NewServerCompetition> findByRoleId(Long roleId);
}
