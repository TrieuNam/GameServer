package com.southMillion.report_service.controller;

import com.southMillion.report_service.entity.ReportEvent;
import com.southMillion.report_service.service.ReportEventService;
import org.SouthMillion.dto.report.ReportEventDTO;
import org.SouthMillion.dto.report.ReportResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportEventService reportEventService;

    @PostMapping("/report")
    public String report(@RequestParam("data") String base64Data) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decodedString.split("\t");

            ReportEventDTO dto = new ReportEventDTO();
            dto.setType(Integer.parseInt(parts[0]));
            dto.setAgentId(parts[1]);
            dto.setDeviceId(parts[2]);
            dto.setPackageVersion(parts[3]);
            dto.setSourceVersion(parts[4]);
            dto.setSessionId(parts[5]);
            dto.setLoginTime(Long.parseLong(parts[6]));
            dto.setNetState(Integer.parseInt(parts[7]));
            dto.setEventTime(Long.parseLong(parts[8]));
            dto.setImea(parts[9]);
            dto.setChannelId(parts[10]);
            if (parts.length > 11) {
                dto.setExtraParams(Arrays.asList(Arrays.copyOfRange(parts, 11, parts.length)));
            }

            // Chuyển DTO sang Entity
            ReportEvent entity = convertToEntity(dto);
            reportEventService.save(entity);
            return "OK";
        } catch (Exception e) {
            return "Invalid data: " + e.getMessage();
        }
    }

    private ReportEvent convertToEntity(ReportEventDTO dto) {
        ReportEvent entity = new ReportEvent();
        entity.setType(dto.getType());
        entity.setAgentId(dto.getAgentId());
        entity.setDeviceId(dto.getDeviceId());
        entity.setPackageVersion(dto.getPackageVersion());
        entity.setSourceVersion(dto.getSourceVersion());
        entity.setSessionId(dto.getSessionId());
        entity.setLoginTime(dto.getLoginTime());
        entity.setNetState(dto.getNetState());
        entity.setEventTime(dto.getEventTime());
        entity.setImea(dto.getImea());
        entity.setChannelId(dto.getChannelId());
        if (dto.getExtraParams() != null && !dto.getExtraParams().isEmpty()) {
            entity.setExtraParams(String.join("\t", dto.getExtraParams()));
        }
        return entity;
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport(@RequestParam String data) {
        try {
            ReportResultDTO dto = reportEventService.processReportToDTO(data);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid data or server error: " + e.getMessage());
        }
    }
}