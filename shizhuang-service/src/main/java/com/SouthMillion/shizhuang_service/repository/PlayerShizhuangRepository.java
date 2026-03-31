package com.SouthMillion.shizhuang_service.repository;

import com.SouthMillion.shizhuang_service.entity.PlayerShizhuang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerShizhuangRepository extends JpaRepository<PlayerShizhuang, Long> {

    List<PlayerShizhuang> findByRoleId(Long roleId);

    Optional<PlayerShizhuang> findByRoleIdAndShizhuangId(Long roleId, int shizhuangId);

    Optional<PlayerShizhuang> findByRoleIdAndWearingTrue(Long roleId);
}
