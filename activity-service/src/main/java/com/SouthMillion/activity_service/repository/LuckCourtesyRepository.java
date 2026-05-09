package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.LuckCourtesy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LuckCourtesyRepository extends JpaRepository<LuckCourtesy, Long>, RoleIdLookupRepository<LuckCourtesy, Long> {
}