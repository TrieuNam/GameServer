package com.SouthMillion.report_service.service;

import com.SouthMillion.report_service.dto.ReportStatsDTO;
import com.SouthMillion.report_service.entity.ReportEvent;
import com.SouthMillion.report_service.repository.ReportEventRepository;
import org.SouthMillion.dto.report.ReportResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class ReportEventService {
    @Autowired
    private ReportEventRepository repository;

    public ReportEvent save(ReportEvent event) {
        return repository.save(event);
    }

    public ReportResultDTO processReportToDTO(String data) {
        try {
            // Giải mã base64
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            // Trả về trạng thái + dữ liệu đã giải mã (raw string, client tự parse)
            return new ReportResultDTO("ok", decodedString);
        } catch (Exception e) {
            // Nếu lỗi, trả về object với trạng thái lỗi
            return new ReportResultDTO("error", null);
        }
    }

    public List<ReportEvent> findByType(int type) {
        return repository.findByType(type);
    }

    public List<ReportEvent> findByDeviceId(String deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public List<ReportEvent> findByDateRange(long startTime, long endTime) {
        return repository.findByEventTimeBetween(startTime, endTime);
    }

    public ReportStatsDTO getStatistics() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        
        return ReportStatsDTO.builder()
                .totalReports(repository.count())
                .todayReports(repository.countByCreatedAtAfter(oneHourAgo))
                .activeDevices(repository.countDistinctDeviceIdByCreatedAtAfter(oneHourAgo))
                .resolutionRate(0.95)
                .build();
    }
}
