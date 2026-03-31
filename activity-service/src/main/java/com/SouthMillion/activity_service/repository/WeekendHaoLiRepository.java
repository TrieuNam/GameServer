package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.WeekendHaoLi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeekendHaoLiRepository extends JpaRepository<WeekendHaoLi, Long> {
    Optional<WeekendHaoLi> findByRoleId(Long roleId);
}
