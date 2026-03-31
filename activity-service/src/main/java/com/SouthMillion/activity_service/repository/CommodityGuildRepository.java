package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CommodityGuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommodityGuildRepository extends JpaRepository<CommodityGuild, Long> {
    Optional<CommodityGuild> findByRoleId(Long roleId);
}
