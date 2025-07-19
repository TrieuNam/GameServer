package com.southMillion.report_service.repository;

import com.southMillion.report_service.entity.ReportEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportEventRepository extends JpaRepository<ReportEvent, Long> {
}