package com.SouthMillion.admin.doctor.controller;

import com.SouthMillion.admin.doctor.dto.DoctorActionRequest;
import com.SouthMillion.admin.doctor.dto.DoctorSessionView;
import com.SouthMillion.admin.doctor.service.DoctorSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for the Service Doctor dashboard.
 */
@RestController
@RequestMapping("/api/doctor/sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DoctorController {

    private final DoctorSessionService doctorSessionService;

    @GetMapping
    public ResponseEntity<List<DoctorSessionView>> getAllSessions() {
        return ResponseEntity.ok(doctorSessionService.getAllSessions());
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<DoctorSessionView> getSession(@PathVariable String serviceName) {
        return doctorSessionService.getSession(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("autoApprovalEnabled", doctorSessionService.isAutoApprovalEnabled());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settings/auto-approval")
    public ResponseEntity<Map<String, Object>> updateAutoApproval(@RequestBody(required = false) DoctorActionRequest request) {
        boolean enabled = request != null && Boolean.TRUE.equals(request.getEnabled());
        doctorSessionService.setAutoApprovalEnabled(enabled);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("autoApprovalEnabled", doctorSessionService.isAutoApprovalEnabled());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{serviceName}/watch")
    public ResponseEntity<DoctorSessionView> watchService(@PathVariable String serviceName) {
        log.info("🩺 Watching doctor session for service: {}", serviceName);
        return doctorSessionService.watchService(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{serviceName}/approve")
    public ResponseEntity<DoctorSessionView> approveService(
            @PathVariable String serviceName,
            @RequestBody(required = false) DoctorActionRequest request) {
        String note = request != null ? request.getNotes() : null;
        log.info("✅ Doctor approval requested for service: {}", serviceName);
        return doctorSessionService.approveService(serviceName, note)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{serviceName}/reject")
    public ResponseEntity<DoctorSessionView> rejectService(
            @PathVariable String serviceName,
            @RequestBody(required = false) DoctorActionRequest request) {
        String note = request != null ? request.getNotes() : null;
        log.info("⛔ Doctor rejection requested for service: {}", serviceName);
        return doctorSessionService.rejectService(serviceName, note)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{serviceName}/prepare-fix")
    public ResponseEntity<DoctorSessionView> prepareFix(
            @PathVariable String serviceName,
            @RequestBody(required = false) DoctorActionRequest request) {
        String note = request != null ? request.getNotes() : null;
        log.info("🤖 Preparing Copilot repair flow for service: {}", serviceName);
        return doctorSessionService.prepareCopilotFix(serviceName, note)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{serviceName}/retry-build")
    public ResponseEntity<DoctorSessionView> retryBuild(
            @PathVariable String serviceName,
            @RequestBody(required = false) DoctorActionRequest request) {
        String note = request != null ? request.getNotes() : null;
        log.info("🏗️ Running build verification for service: {}", serviceName);
        return doctorSessionService.retryBuild(serviceName, note)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{serviceName}/auto-fix")
    public ResponseEntity<DoctorSessionView> autoFixAndRestart(
            @PathVariable String serviceName,
            @RequestBody(required = false) DoctorActionRequest request) {
        String note = request != null ? request.getNotes() : null;
        String errorType = request != null ? request.getErrorType() : null;
        String errorSummary = request != null ? request.getErrorSummary() : null;
        List<String> errorLogs = request != null ? request.getErrorLogs() : null;

        log.info("🤖 Auto fix and restart requested for service: {}", serviceName);
        return doctorSessionService.autoFixAndRestartService(serviceName, note, errorType, errorSummary, errorLogs)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{serviceName}/events")
    public SseEmitter streamSessionEvents(@PathVariable String serviceName) {
        return doctorSessionService.openEventStream(serviceName);
    }
}
