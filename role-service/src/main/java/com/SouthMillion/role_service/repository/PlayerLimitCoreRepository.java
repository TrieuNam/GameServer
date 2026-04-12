package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.PlayerLimitCore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerLimitCoreRepository extends JpaRepository<PlayerLimitCore, Long> {

    List<PlayerLimitCore> findByRoleId(Long roleId);

    Optional<PlayerLimitCore> findByRoleIdAndLimitType(Long roleId, Integer limitType);
}
