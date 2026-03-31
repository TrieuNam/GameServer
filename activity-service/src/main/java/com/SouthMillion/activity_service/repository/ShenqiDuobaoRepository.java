package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.ShenqiDuobao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShenqiDuobaoRepository extends JpaRepository<ShenqiDuobao, Long> {
    Optional<ShenqiDuobao> findByRoleId(Long roleId);
}
