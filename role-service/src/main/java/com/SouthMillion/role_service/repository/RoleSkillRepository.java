package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.RoleSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleSkillRepository extends JpaRepository<RoleSkill, Long> {

    List<RoleSkill> findByRoleId(Long roleId);

    List<RoleSkill> findByRoleIdOrderBySkillIndexAscSkillIdAsc(Long roleId);

    Optional<RoleSkill> findByRoleIdAndSkillId(Long roleId, Integer skillId);

    boolean existsByRoleIdAndSkillId(Long roleId, Integer skillId);

    /** Tăng cấp tất cả kỹ năng của một nhân vật lên 1 (one-key level up). */
    @Modifying
    @Query("UPDATE RoleSkill s SET s.skillLevel = s.skillLevel + 1 WHERE s.roleId = :roleId AND s.skillLevel < :maxLevel")
    int incrementAllLevels(@Param("roleId") Long roleId, @Param("maxLevel") Integer maxLevel);
}

