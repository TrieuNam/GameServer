package com.southMillion.report_service.repository;

import com.southMillion.report_service.entity.ReportLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {
}