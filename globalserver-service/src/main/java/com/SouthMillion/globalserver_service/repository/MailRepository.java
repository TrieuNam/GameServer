package com.SouthMillion.globalserver_service.repository;

import com.SouthMillion.globalserver_service.entity.MailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailRepository extends JpaRepository<MailEntity, Integer> {
    List<MailEntity> findByUserId(Long userId);
}
