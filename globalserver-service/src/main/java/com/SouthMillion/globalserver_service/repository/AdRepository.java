package com.SouthMillion.globalserver_service.repository;

import com.SouthMillion.globalserver_service.entity.AdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdRepository extends JpaRepository<AdEntity, Long> {}