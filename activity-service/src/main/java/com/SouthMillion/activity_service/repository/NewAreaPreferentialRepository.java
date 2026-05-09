package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.NewAreaPreferential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewAreaPreferentialRepository extends JpaRepository<NewAreaPreferential, Long> {
    List<NewAreaPreferential> findAll();
}