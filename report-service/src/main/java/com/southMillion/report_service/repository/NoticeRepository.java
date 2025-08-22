package com.southMillion.report_service.repository;

import com.southMillion.report_service.entity.NoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeEntity, Long> {
    // Thêm các truy vấn custom nếu cần
}