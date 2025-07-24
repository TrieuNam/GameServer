package com.southMillion.report_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.southMillion.report_service.entity.ReportLog;
import com.southMillion.report_service.repository.ReportLogRepository;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportLogService {
    private final ReportLogRepository repo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(KafkaEventDto event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event.getPayload());
            ReportLog log = ReportLog.builder()
                    .userId(event.getUserId())
                    .eventType(event.getType())
                    .payloadJson(payloadJson)
                    .timestamp(event.getTimestamp())
                    .build();
            repo.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}