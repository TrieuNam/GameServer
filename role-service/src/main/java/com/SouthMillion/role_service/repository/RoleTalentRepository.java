package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.RoleTalent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTalentRepository extends JpaRepository<RoleTalent, Long> {

    List<RoleTalent> findByRoleId(Long roleId);

    List<RoleTalent> findByRoleIdOrderBySkillIdAsc(Long roleId);

    Optional<RoleTalent> findByRoleIdAndSkillId(Long roleId, Integer skillId);

    boolean existsByRoleIdAndSkillId(Long roleId, Integer skillId);
}

