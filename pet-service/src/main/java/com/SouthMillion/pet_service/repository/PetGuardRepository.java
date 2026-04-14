package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.model.entity.PetGuardState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PetGuardRepository extends JpaRepository<PetGuardState, Long> {
    Optional<PetGuardState> findByRoleId(Long roleId);
}
