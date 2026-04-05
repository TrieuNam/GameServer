package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.doctor.model.DoctorSessionStatus;
import com.SouthMillion.admin.entity.ServiceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorClassifierServiceTest {

    private final ErrorClassifierService service = new ErrorClassifierService();

    @Test
    void shouldFlagRuntimeExceptionAsErrorEvenWhenServiceStillRunning() {
        ErrorClassifierService.ClassifiedError result = service.classify(
                ServiceStatus.RUNNING,
                List.of(
                        "2026-04-05 14:00:00 INFO Started RoleServiceApplication in 8.123 seconds",
                        "2026-04-05 14:05:10 ERROR [http-nio-8080-exec-4] java.lang.NullPointerException: roleId is null"
                )
        );

        assertEquals(DoctorSessionStatus.ERROR, result.status());
        assertEquals("RuntimeException", result.errorType());
        assertTrue(result.summary().contains("NullPointerException"));
        assertTrue(result.approvalRequired());
    }

    @Test
    void shouldReturnWaitingWhenServiceIsRetryingDependencies() {
        ErrorClassifierService.ClassifiedError result = service.classify(
                ServiceStatus.STARTING,
                List.of(
                        "2026-04-05 14:00:00 INFO Boot sequence started",
                        "2026-04-05 14:00:05 WARN Waiting for world-service registration before continuing startup"
                )
        );

        assertEquals(DoctorSessionStatus.WAITING, result.status());
        assertEquals("WaitingForDependency", result.errorType());
        assertTrue(result.summary().contains("Waiting for world-service"));
    }

    @Test
    void shouldPreferErrorStateOverOldStartedLine() {
        ErrorClassifierService.ClassifiedError result = service.classify(
                ServiceStatus.ERROR,
                List.of(
                        "2026-04-05 14:00:00 INFO Started EquipServiceApplication in 7.321 seconds",
                        "2026-04-05 14:09:44 WARN graceful shutdown in progress"
                )
        );

        assertEquals(DoctorSessionStatus.ERROR, result.status());
        assertEquals("UnhandledError", result.errorType());
    }

    @Test
    void shouldIgnoreOptionalValidatorProviderWarningForHealthyRunningService() {
        ErrorClassifierService.ClassifiedError result = service.classify(
                ServiceStatus.RUNNING,
                List.of(
                        "2026-04-05 15:24:29.677+07:00  INFO 17988 --- [config-service] [           main] o.s.v.b.OptionalValidatorFactoryBean     : Failed to set up a Bean Validation provider: jakarta.validation.NoProviderFoundException: Unable to create a Configuration, because no Jakarta Bean Validation provider could be found.",
                        "2026-04-05 15:24:31.102+07:00  INFO 17988 --- [config-service] [           main] c.S.config.ConfigServiceApplication      : Started ConfigServiceApplication in 6.2 seconds"
                )
        );

        assertEquals(DoctorSessionStatus.STARTED, result.status());
        assertEquals("Service is running normally", result.summary());
        assertTrue(!result.approvalRequired());
    }
}
