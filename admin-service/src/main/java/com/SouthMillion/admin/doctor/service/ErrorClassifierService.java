package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.doctor.model.DoctorSessionStatus;
import com.SouthMillion.admin.entity.ServiceStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Classifies recent service logs into startup, build, and runtime issues.
 */
@Service
public class ErrorClassifierService {

    private static final List<Pattern> BENIGN_PATTERNS = List.of(
            Pattern.compile("OptionalValidatorFactoryBean.*Bean Validation provider", Pattern.CASE_INSENSITIVE),
            Pattern.compile("NoProviderFoundException: Unable to create a Configuration, because no Jakarta Bean Validation provider could be found", Pattern.CASE_INSENSITIVE)
    );

    private static final List<ErrorRule> ERROR_RULES = List.of(
            new ErrorRule("PortInUse", Pattern.compile("port\\s+\\d+\\s+was already in use|address already in use", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("BeanCreationException", Pattern.compile("BeanCreationException|UnsatisfiedDependencyException", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("ApplicationContextException", Pattern.compile("ApplicationContextException", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("FlywayException", Pattern.compile("FlywayException|Validate failed|Schema history", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("DatabaseConnection", Pattern.compile("Communications link failure|JDBCConnectionException|Access denied for user|could not open jdbc connection", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("RedisConnection", Pattern.compile("RedisConnectionFailureException|Unable to connect to Redis|NOAUTH Authentication required", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("KafkaException", Pattern.compile("KafkaException|Timeout expired while fetching topic metadata|Bootstrap broker", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("ClassLoading", Pattern.compile("ClassNotFoundException|NoSuchMethodError|NoClassDefFoundError", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("CompilationError", Pattern.compile("COMPILATION ERROR|cannot find symbol|incompatible types|package .* does not exist", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("ConnectionRefused", Pattern.compile("Connection refused|ConnectException", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("RuntimeException", Pattern.compile("NullPointerException|IllegalStateException|IllegalArgumentException|IndexOutOfBoundsException|ConcurrentModificationException|StackOverflowError|OutOfMemoryError|RejectedExecutionException|TimeoutException|SocketTimeoutException|Read timed out|Broken pipe|lock wait timeout exceeded|deadlock found when trying to get lock", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("Http5xx", Pattern.compile("500 Internal Server Error|HTTP\\s*500|status\\s*=\\s*500", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("WebSocketRuntime", Pattern.compile("WebSocket.*(closed|error)|Channel.*exceptionCaught|io\\.netty\\.handler\\.timeout", Pattern.CASE_INSENSITIVE))
    );

    private static final List<ErrorRule> WAITING_RULES = List.of(
            new ErrorRule("WaitingForDependency", Pattern.compile("waiting for|still waiting|awaiting|not ready yet|retrying( to)?|will retry|sleep before retry|backing off|pending dependency", Pattern.CASE_INSENSITIVE)),
            new ErrorRule("DependencyUnavailable", Pattern.compile("service unavailable.*retry|connection .* timed out.*retry|bootstrap .* in progress", Pattern.CASE_INSENSITIVE))
    );

    public ClassifiedError classify(ServiceStatus serviceStatus, List<String> recentLogs) {
        List<String> safeLogs = recentLogs == null ? List.of() : recentLogs;

        for (ErrorRule rule : ERROR_RULES) {
            String matchedLine = findLastMatchingLine(safeLogs, rule.pattern());
            if (matchedLine != null) {
                return new ClassifiedError(
                        DoctorSessionStatus.ERROR,
                        rule.type(),
                        shorten(matchedLine),
                        true
                );
            }
        }

        if (serviceStatus == ServiceStatus.ERROR) {
            return new ClassifiedError(
                    DoctorSessionStatus.ERROR,
                    "UnhandledError",
                    fallbackSummary(safeLogs, "Service entered ERROR state without a known signature"),
                    true
            );
        }

        String suspiciousRuntimeLine = findLastSuspiciousRuntimeLine(safeLogs);
        if (suspiciousRuntimeLine != null) {
            return new ClassifiedError(
                    DoctorSessionStatus.ERROR,
                    "RuntimeException",
                    shorten(suspiciousRuntimeLine),
                    true
            );
        }

        for (ErrorRule rule : WAITING_RULES) {
            String matchedLine = findLastMatchingLine(safeLogs, rule.pattern());
            if (matchedLine != null) {
                return new ClassifiedError(
                        DoctorSessionStatus.WAITING,
                        rule.type(),
                        shorten(matchedLine),
                        false
                );
            }
        }

        if (containsStartedSignal(safeLogs) || serviceStatus == ServiceStatus.RUNNING) {
            return new ClassifiedError(
                    DoctorSessionStatus.STARTED,
                    null,
                    "Service is running normally",
                    false
            );
        }

        if (serviceStatus == ServiceStatus.STARTING) {
            return new ClassifiedError(
                    DoctorSessionStatus.WAITING,
                    "StartingUp",
                    fallbackSummary(safeLogs, "Service is still starting; waiting for more logs"),
                    false
            );
        }

        if (serviceStatus == ServiceStatus.STOPPING) {
            return new ClassifiedError(
                    DoctorSessionStatus.WAITING,
                    "Stopping",
                    "Service is stopping",
                    false
            );
        }

        if (serviceStatus == ServiceStatus.STOPPED) {
            return new ClassifiedError(
                    DoctorSessionStatus.IDLE,
                    null,
                    "Service is currently stopped",
                    false
            );
        }

        return new ClassifiedError(
                DoctorSessionStatus.WAITING,
                null,
                fallbackSummary(safeLogs, "Waiting for service output"),
                false
        );
    }

    private boolean containsStartedSignal(List<String> logs) {
        return logs.stream()
                .map(line -> line.toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.contains("started ")
                        || line.contains("started successfully")
                        || line.contains("tomcat started on port")
                        || line.contains("netty started on port"));
    }

    private String findLastMatchingLine(List<String> logs, Pattern pattern) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            String line = logs.get(i);
            if (isBenignLine(line)) {
                continue;
            }
            if (pattern.matcher(line).find()) {
                return line;
            }
        }
        return null;
    }

    private String findLastSuspiciousRuntimeLine(List<String> logs) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            String line = logs.get(i);
            if (line == null || line.isBlank() || isBenignLine(line)) {
                continue;
            }

            String normalized = line.toLowerCase(Locale.ROOT);
            if (normalized.contains(" started ")
                    || normalized.contains("started successfully")
                    || normalized.contains("graceful shutdown")) {
                continue;
            }

            boolean looksBad = normalized.contains(" exception")
                    || normalized.contains("[error]")
                    || normalized.contains(" error ")
                    || normalized.contains(" failed ")
                    || normalized.startsWith("error ");

            boolean benign = normalized.contains("0 error")
                    || normalized.contains("errorpage")
                    || normalized.contains("no error");

            if (looksBad && !benign) {
                return line;
            }
        }
        return null;
    }

    private boolean isBenignLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        return BENIGN_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(line).find());
    }

    private String fallbackSummary(List<String> logs, String defaultMessage) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            String line = logs.get(i);
            if (line != null && !line.isBlank()) {
                return shorten(line);
            }
        }
        return defaultMessage;
    }

    private String shorten(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 220 ? trimmed.substring(0, 217) + "..." : trimmed;
    }

    private record ErrorRule(String type, Pattern pattern) {
    }

    public record ClassifiedError(
            DoctorSessionStatus status,
            String errorType,
            String summary,
            boolean approvalRequired
    ) {
    }
}
