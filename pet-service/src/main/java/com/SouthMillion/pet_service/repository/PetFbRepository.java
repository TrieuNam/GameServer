package com.SouthMillion.pet_service.repository;

import com.SouthMillion.pet_service.entity.PetFbEntity;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PetFbRepository extends JpaRepository<PetFbEntity, Long> {}
