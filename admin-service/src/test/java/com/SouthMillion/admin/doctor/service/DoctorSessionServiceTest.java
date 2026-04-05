package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.doctor.dto.DoctorSessionView;
import com.SouthMillion.admin.doctor.model.DoctorSessionStatus;
import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import com.SouthMillion.admin.repository.ServiceConfigRepository;
import com.SouthMillion.admin.service.ServiceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorSessionServiceTest {

    @Mock private ServiceConfigRepository configRepository;
    @Mock private ServiceManager serviceManager;
    @Mock private ErrorClassifierService errorClassifierService;
    @Mock private CopilotCliService copilotCliService;
    @Mock private BuildVerificationService buildVerificationService;

    @InjectMocks private DoctorSessionService doctorSessionService;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(doctorSessionService, "objectMapper", new ObjectMapper());
        Path reportDir = Files.createTempDirectory("doctor-session-test");
        ReflectionTestUtils.setField(doctorSessionService, "reportDir", reportDir.toString());
        doctorSessionService.initialize();
    }

    @Test
    void autoApprovalEnabled_promotesNeedsApprovalSessionsToApproved() {
        ServiceConfig config = new ServiceConfig();
        config.setServiceName("role-service");
        config.setDisplayName("Role Service");
        config.setPhase("P2");
        config.setPort(9010);

        when(configRepository.findAllByStartupOrder()).thenReturn(List.of(config));
        when(serviceManager.getServiceLogs("role-service", 80)).thenReturn(List.of("java.lang.NullPointerException: boom"));
        when(serviceManager.getServiceStatus("role-service")).thenReturn(ServiceStatus.ERROR);
        when(errorClassifierService.classify(any(), any())).thenReturn(
                new ErrorClassifierService.ClassifiedError(
                        DoctorSessionStatus.NEEDS_APPROVAL,
                        "RuntimeException",
                        "NullPointerException: boom",
                        true
                )
        );

        doctorSessionService.setAutoApprovalEnabled(true);
        DoctorSessionView session = doctorSessionService.getAllSessions().get(0);

        assertEquals("APPROVED", session.getDoctorStatus());
        assertFalse(session.isApprovalRequired());
        assertTrue(session.isCopilotReady());
    }

    @Test
    void autoFixAndRestartService_enabled_runsStopBuildAndRestart() {
        ServiceConfig config = new ServiceConfig();
        config.setServiceName("wallet-service");
        config.setDisplayName("Wallet Service");
        config.setPhase("P2");
        config.setPort(9020);

        when(configRepository.findByServiceName("wallet-service")).thenReturn(Optional.of(config));
        when(serviceManager.getServiceLogs("wallet-service", 80)).thenReturn(List.of("java.lang.IllegalStateException: bad state"));
        when(serviceManager.getServiceStatus("wallet-service")).thenReturn(ServiceStatus.ERROR);
        when(errorClassifierService.classify(any(), any())).thenReturn(
                new ErrorClassifierService.ClassifiedError(
                        DoctorSessionStatus.NEEDS_APPROVAL,
                        "RuntimeException",
                        "IllegalStateException: bad state",
                        true
                )
        );
        when(copilotCliService.prepareFix(any(), any(), any())).thenReturn(
                new CopilotCliService.CopilotResult(false, false, "Prompt prepared", "prompt.md", "copilot.txt", "gh copilot")
        );
        when(buildVerificationService.verifyBuild(any(), any())).thenReturn(
                new BuildVerificationService.BuildResult(true, "Build verification passed.", "build.txt", "mvn -DskipTests compile")
        );
        when(serviceManager.restartService("wallet-service")).thenReturn(CompletableFuture.completedFuture(true));
        when(serviceManager.stopService("wallet-service")).thenReturn(true);

        doctorSessionService.setAutoApprovalEnabled(true);
        DoctorSessionView session = doctorSessionService.autoFixAndRestartService(
                "wallet-service",
                "auto fix now",
                "GrpcTimeout",
                "rpc timeout while bootstrap",
                List.of("io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED", "bootstrap role sync failed")
        ).orElseThrow();

        ArgumentCaptor<DoctorSessionView> sessionCaptor = ArgumentCaptor.forClass(DoctorSessionView.class);
        verify(serviceManager).stopService("wallet-service");
        verify(copilotCliService).prepareFix(any(), sessionCaptor.capture(), any());
        verify(buildVerificationService).verifyBuild(any(), any());
        verify(serviceManager).restartService("wallet-service");
        assertEquals("GrpcTimeout", sessionCaptor.getValue().getLastErrorType());
        assertEquals("rpc timeout while bootstrap", sessionCaptor.getValue().getLastErrorSummary());
        assertEquals(2, sessionCaptor.getValue().getRecentLogs().size());
        assertEquals("VERIFIED", session.getDoctorStatus());
        assertFalse(session.isApprovalRequired());
    }
}
