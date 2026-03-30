package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.LianChongZengLi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LianChongZengLiRepository extends JpaRepository<LianChongZengLi, Long> {
    Optional<LianChongZengLi> findByRoleId(Long roleId);
}
