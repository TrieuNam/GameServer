package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.NewServerGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewServerGlobalRepository extends JpaRepository<NewServerGlobal, Long> {
    Optional<NewServerGlobal> findByServerId(Integer serverId);
}
