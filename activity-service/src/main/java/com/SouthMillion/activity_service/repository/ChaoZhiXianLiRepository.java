package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.ChaoZhiXianLi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChaoZhiXianLiRepository extends JpaRepository<ChaoZhiXianLi, Long> {
    Optional<ChaoZhiXianLi> findByRoleId(Long roleId);
}
