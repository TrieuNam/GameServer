package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.BagMeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BagMetaRepository extends JpaRepository<BagMeta, BagMeta.BagMetaId> { }