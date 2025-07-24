package com.SouthMillion.globalserver_service.repository;

import com.SouthMillion.globalserver_service.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {}