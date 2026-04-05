package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.doctor.dto.DoctorSessionView;
import com.SouthMillion.admin.doctor.model.DoctorSessionStatus;
import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import com.SouthMillion.admin.repository.ServiceConfigRepository;
import com.SouthMillion.admin.service.ServiceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds live Service Doctor sessions from current service status and recent logs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorSessionService {

    private final ServiceConfigRepository configRepository;
    private final ServiceManager serviceManager;
    private final ErrorClassifierService errorClassifierService;
    private final CopilotCliService copilotCliService;
    private final BuildVerificationService buildVerificationService;
    private final ObjectMapper objectMapper;

    private final Set<String> watchedServices = ConcurrentHashMap.newKeySet();
    private final Map<String, SessionOverride> sessionOverrides = new ConcurrentHashMap<>();
    private final Map<SseEmitter, String> emitters = new ConcurrentHashMap<>();

    @Value("${doctor.report-dir:monitoring/reports}")
    private String reportDir;

    @Value("${doctor.auto-approval-enabled:false}")
    private boolean configuredAutoApprovalEnabled;

    private final AtomicBoolean autoApprovalEnabled = new AtomicBoolean(false);

    @PostConstruct
    public void initialize() {
        autoApprovalEnabled.set(configuredAutoApprovalEnabled);
        try {
            Files.createDirectories(getReportDirectory());
        } catch (IOException e) {
            log.warn("Could not create doctor report directory: {}", reportDir, e);
        }
    }

    public List<DoctorSessionView> getAllSessions() {
        return configRepository.findAllByStartupOrder().stream()
                .map(this::buildSession)
                .toList();
    }

    public Optional<DoctorSessionView> getSession(String serviceName) {
        return configRepository.findByServiceName(serviceName)
                .map(this::buildSession);
    }

    public Optional<DoctorSessionView> watchService(String serviceName) {
        watchedServices.add(serviceName);
        return getSession(serviceName);
    }

    public boolean isAutoApprovalEnabled() {
        return autoApprovalEnabled.get();
    }

    public void setAutoApprovalEnabled(boolean enabled) {
        autoApprovalEnabled.set(enabled);
        log.info("🤖 Service Doctor auto approval switched {}", enabled ? "ON" : "OFF");
    }

    public Optional<DoctorSessionView> approveService(String serviceName, String note) {
        watchedServices.add(serviceName);
        sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                existing,
                DoctorSessionStatus.APPROVED,
                defaultNote(note, "Approved by admin. Ready to prepare Copilot CLI prompt."),
                null,
                null,
                null
        ));
        return getSession(serviceName);
    }

    public Optional<DoctorSessionView> rejectService(String serviceName, String note) {
        watchedServices.add(serviceName);
        sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                existing,
                DoctorSessionStatus.REJECTED,
                defaultNote(note, "Rejected by admin. Keep monitoring only."),
                null,
                null,
                null
        ));
        return getSession(serviceName);
    }

    public Optional<DoctorSessionView> prepareCopilotFix(String serviceName, String note) {
        return configRepository.findByServiceName(serviceName).map(config -> {
            watchedServices.add(serviceName);
            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    DoctorSessionStatus.FIXING,
                    defaultNote(note, "Preparing Copilot repair prompt."),
                    null,
                    null,
                    null
            ));

            DoctorSessionView currentSession = buildSession(config);
            CopilotCliService.CopilotResult result = copilotCliService.prepareFix(config, currentSession, getReportDirectory());

            DoctorSessionStatus finalStatus = result.executed()
                    ? (result.success() ? DoctorSessionStatus.FIXING : DoctorSessionStatus.FAILED)
                    : DoctorSessionStatus.APPROVED;

            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    finalStatus,
                    result.message(),
                    result.promptFile(),
                    result.outputFile(),
                    result.command()
            ));

            return buildSession(config);
        });
    }

    public Optional<DoctorSessionView> retryBuild(String serviceName, String note) {
        return configRepository.findByServiceName(serviceName).map(config -> {
            watchedServices.add(serviceName);
            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    DoctorSessionStatus.BUILDING,
                    defaultNote(note, "Running local build verification."),
                    null,
                    existing != null ? existing.reportFile() : null,
                    null
            ));

            BuildVerificationService.BuildResult result = buildVerificationService.verifyBuild(config, getReportDirectory());
            DoctorSessionStatus finalStatus = result.success() ? DoctorSessionStatus.VERIFIED : DoctorSessionStatus.FAILED;

            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    finalStatus,
                    result.message(),
                    null,
                    result.reportFile(),
                    result.command()
            ));

            return buildSession(config);
        });
    }

    public Optional<DoctorSessionView> autoFixAndRestartService(String serviceName, String note) {
        return autoFixAndRestartService(serviceName, note, null, null, null);
    }

    public Optional<DoctorSessionView> autoFixAndRestartService(
            String serviceName,
            String note,
            String errorType,
            String errorSummary,
            List<String> errorLogs
    ) {
        return configRepository.findByServiceName(serviceName).map(config -> {
            watchedServices.add(serviceName);

            String effectiveNote = buildIssueAwareNote(note, errorType, errorSummary, errorLogs);

            if (!isAutoApprovalEnabled()) {
                sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                        existing,
                        DoctorSessionStatus.NEEDS_APPROVAL,
                        defaultNote(effectiveNote, "Auto approval is OFF. Turn it ON to allow auto fix and restart."),
                        null,
                        null,
                        null
                ));
                return buildSession(config);
            }

            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    DoctorSessionStatus.FIXING,
                    defaultNote(effectiveNote, "Auto approval is ON. Service Doctor is stopping the service, preparing a fix, verifying the build, and restarting it."),
                    null,
                    null,
                    null
            ));

            boolean stopped = serviceManager.stopService(serviceName);
            DoctorSessionView currentSession = buildSession(config);
            DoctorSessionView enrichedSession = enrichSessionWithRequestContext(currentSession, effectiveNote, errorType, errorSummary, errorLogs);
            CopilotCliService.CopilotResult copilotResult = copilotCliService.prepareFix(config, enrichedSession, getReportDirectory());
            BuildVerificationService.BuildResult buildResult = buildVerificationService.verifyBuild(config, getReportDirectory());

            boolean restartSucceeded = false;
            if (buildResult.success()) {
                try {
                    restartSucceeded = Boolean.TRUE.equals(serviceManager.restartService(serviceName).get(120, TimeUnit.SECONDS));
                } catch (Exception e) {
                    log.warn("Could not restart service {} after auto fix flow", serviceName, e);
                }
            }

            DoctorSessionStatus finalStatus = buildResult.success() && restartSucceeded
                    ? DoctorSessionStatus.VERIFIED
                    : DoctorSessionStatus.FAILED;

            String notePrefix = (effectiveNote == null || effectiveNote.isBlank()) ? "" : effectiveNote.trim() + " ";
            String finalNote = notePrefix
                    + "Auto approval ON. "
                    + (stopped ? "Service stopped. " : "Service stop skipped or failed. ")
                    + (copilotResult.message() != null ? copilotResult.message() + " " : "")
                    + buildResult.message() + " "
                    + (restartSucceeded ? "Service restarted successfully." : "Service restart did not complete successfully.");

            String lastCommand = restartSucceeded
                    ? "restartService(" + serviceName + ")"
                    : (buildResult.command() != null ? buildResult.command() : copilotResult.command());

            sessionOverrides.compute(serviceName, (name, existing) -> mergeOverride(
                    existing,
                    finalStatus,
                    finalNote.trim(),
                    copilotResult.promptFile(),
                    buildResult.reportFile() != null ? buildResult.reportFile() : copilotResult.outputFile(),
                    lastCommand
            ));

            return buildSession(config);
        });
    }

    public SseEmitter openEventStream(String serviceName) {
        watchedServices.add(serviceName);
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(emitter, serviceName);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        getSession(serviceName).ifPresent(session -> sendEvent(emitter, session));
        return emitter;
    }

    @Scheduled(fixedDelayString = "${doctor.refresh-interval-ms:5000}")
    public void refreshAndBroadcast() {
        configRepository.findAllByStartupOrder().forEach(this::buildSession);
        emitters.forEach((emitter, serviceName) -> getSession(serviceName).ifPresent(session -> sendEvent(emitter, session)));
    }

    private DoctorSessionView buildSession(ServiceConfig config) {
        String serviceName = config.getServiceName();
        List<String> recentLogs = serviceManager.getServiceLogs(serviceName, 80);
        ServiceStatus serviceStatus = serviceManager.getServiceStatus(serviceName);

        ErrorClassifierService.ClassifiedError classified = errorClassifierService.classify(serviceStatus, recentLogs);
        SessionOverride override = sessionOverrides.get(serviceName);

        DoctorSessionStatus doctorStatus = classified.status();
        boolean approvalRequired = classified.approvalRequired();
        String decisionNote = null;
        String promptFile = null;
        String reportFile = null;
        String lastCommand = null;
        LocalDateTime lastUpdated = LocalDateTime.now();

        if (override != null) {
            decisionNote = override.note();
            promptFile = override.promptFile();
            reportFile = override.reportFile();
            lastCommand = override.lastCommand();
            lastUpdated = override.updatedAt();

            switch (override.status()) {
                case REJECTED -> {
                    doctorStatus = DoctorSessionStatus.REJECTED;
                    approvalRequired = false;
                }
                case APPROVED, FIXING, BUILDING, VERIFIED, FAILED -> {
                    doctorStatus = override.status();
                    approvalRequired = false;
                }
                default -> {
                    doctorStatus = override.status();
                    approvalRequired = doctorStatus == DoctorSessionStatus.NEEDS_APPROVAL;
                }
            }
        }

        if (isAutoApprovalEnabled()
                && approvalRequired
                && doctorStatus == DoctorSessionStatus.NEEDS_APPROVAL
                && (override == null || override.status() != DoctorSessionStatus.REJECTED)) {
            doctorStatus = DoctorSessionStatus.APPROVED;
            approvalRequired = false;
            decisionNote = (decisionNote == null || decisionNote.isBlank())
                    ? "Auto approval is ON. Service Doctor may continue the fix and restart flow without manual approval."
                    : decisionNote;
        }

        DoctorSessionView session = DoctorSessionView.builder()
                .serviceName(serviceName)
                .displayName(config.getDisplayName() != null ? config.getDisplayName() : serviceName)
                .phase(config.getPhase())
                .port(config.getPort())
                .serviceStatus(serviceStatus.name())
                .doctorStatus(doctorStatus.name())
                .watched(watchedServices.contains(serviceName))
                .approvalRequired(approvalRequired)
                .autoApprovalEnabled(isAutoApprovalEnabled())
                .copilotReady(doctorStatus == DoctorSessionStatus.APPROVED
                        || doctorStatus == DoctorSessionStatus.FIXING
                        || doctorStatus == DoctorSessionStatus.FAILED
                        || doctorStatus == DoctorSessionStatus.VERIFIED)
                .lastErrorType(classified.errorType())
                .lastErrorSummary(classified.summary())
                .decisionNote(decisionNote)
                .promptFile(promptFile)
                .reportFile(reportFile)
                .lastCommand(lastCommand)
                .recentLogs(trimLogs(recentLogs, 12))
                .lastUpdated(lastUpdated)
                .build();

        persistReport(session);
        return session;
    }

    private List<String> trimLogs(List<String> logs, int maxLines) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        if (logs.size() <= maxLines) {
            return List.copyOf(logs);
        }
        return List.copyOf(logs.subList(logs.size() - maxLines, logs.size()));
    }

    private void persistReport(DoctorSessionView session) {
        try {
            Path reportPath = getReportDirectory().resolve(session.getServiceName() + "-doctor.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), session);
        } catch (IOException e) {
            log.debug("Could not write doctor report for {}", session.getServiceName(), e);
        }
    }

    private void sendEvent(SseEmitter emitter, DoctorSessionView session) {
        try {
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data(session));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.complete();
        }
    }

    private Path getReportDirectory() {
        return Path.of(reportDir);
    }

    private String defaultNote(String note, String defaultValue) {
        return (note == null || note.isBlank()) ? defaultValue : note.trim();
    }

    private String buildIssueAwareNote(String note, String errorType, String errorSummary, List<String> errorLogs) {
        StringBuilder sb = new StringBuilder();
        if (note != null && !note.isBlank()) {
            sb.append(note.trim());
        }
        if (errorType != null && !errorType.isBlank()) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append("Error type: ").append(errorType.trim());
        }
        if (errorSummary != null && !errorSummary.isBlank()) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append("Error summary: ").append(errorSummary.trim());
        }
        if (errorLogs != null && !errorLogs.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append("Recent error logs:").append(System.lineSeparator())
                    .append(String.join(System.lineSeparator(), trimLogs(errorLogs, 12)));
        }
        return sb.toString();
    }

    private DoctorSessionView enrichSessionWithRequestContext(
            DoctorSessionView session,
            String effectiveNote,
            String errorType,
            String errorSummary,
            List<String> errorLogs
    ) {
        if (session == null) {
            return null;
        }

        List<String> mergedLogs = (errorLogs != null && !errorLogs.isEmpty())
                ? trimLogs(errorLogs, 12)
                : session.getRecentLogs();

        String mergedType = (errorType == null || errorType.isBlank()) ? session.getLastErrorType() : errorType.trim();
        String mergedSummary = (errorSummary == null || errorSummary.isBlank()) ? session.getLastErrorSummary() : errorSummary.trim();
        String mergedDecisionNote = (effectiveNote == null || effectiveNote.isBlank()) ? session.getDecisionNote() : effectiveNote;

        return DoctorSessionView.builder()
                .serviceName(session.getServiceName())
                .displayName(session.getDisplayName())
                .phase(session.getPhase())
                .port(session.getPort())
                .serviceStatus(session.getServiceStatus())
                .doctorStatus(session.getDoctorStatus())
                .watched(session.isWatched())
                .approvalRequired(session.isApprovalRequired())
                .autoApprovalEnabled(session.isAutoApprovalEnabled())
                .copilotReady(session.isCopilotReady())
                .lastErrorType(mergedType)
                .lastErrorSummary(mergedSummary)
                .decisionNote(mergedDecisionNote)
                .promptFile(session.getPromptFile())
                .reportFile(session.getReportFile())
                .lastCommand(session.getLastCommand())
                .recentLogs(mergedLogs)
                .lastUpdated(session.getLastUpdated())
                .build();
    }

    private SessionOverride mergeOverride(
            SessionOverride existing,
            DoctorSessionStatus status,
            String note,
            String promptFile,
            String reportFile,
            String lastCommand
    ) {
        return new SessionOverride(
                status,
                note != null ? note : existing != null ? existing.note() : null,
                promptFile != null ? promptFile : existing != null ? existing.promptFile() : null,
                reportFile != null ? reportFile : existing != null ? existing.reportFile() : null,
                lastCommand != null ? lastCommand : existing != null ? existing.lastCommand() : null,
                LocalDateTime.now()
        );
    }

    private record SessionOverride(
            DoctorSessionStatus status,
            String note,
            String promptFile,
            String reportFile,
            String lastCommand,
            LocalDateTime updatedAt
    ) {
    }
}
