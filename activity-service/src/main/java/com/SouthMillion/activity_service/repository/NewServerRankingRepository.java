package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.NewServerRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewServerRankingRepository extends JpaRepository<NewServerRanking, Long> {
    Optional<NewServerRanking> findByRoleId(Long roleId);
}
