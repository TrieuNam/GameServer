package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.JifenZhuanpan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JifenZhuanpanRepository extends JpaRepository<JifenZhuanpan, Long> {
    Optional<JifenZhuanpan> findByRoleId(Long roleId);
}
