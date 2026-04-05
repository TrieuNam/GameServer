package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.entity.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs a local build verification command for a selected service.
 */
@Service
@Slf4j
public class BuildVerificationService {

    @Value("${gameserver.base.path:D:/project/serverGame/GameServer}")
    private String baseGameServerPath;

    @Value("${doctor.build.command:mvn -DskipTests compile}")
    private String defaultBuildCommand;

    public BuildResult verifyBuild(ServiceConfig config, Path reportDirectory) {
        try {
            Files.createDirectories(reportDirectory);
            Path workingDirectory = resolveWorkingDirectory(config);

            if (!Files.exists(workingDirectory)) {
                return new BuildResult(false,
                        "Working directory does not exist: " + workingDirectory,
                        null,
                        defaultBuildCommand);
            }

            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", defaultBuildCommand)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            Path reportFile = reportDirectory.resolve(config.getServiceName() + "-build.txt");
            Files.writeString(reportFile, output.toString(), StandardCharsets.UTF_8);

            boolean success = exitCode == 0;
            String message = success
                    ? "Build verification passed."
                    : "Build verification failed. Review the build report for details.";

            return new BuildResult(success, message, reportFile.toString(), defaultBuildCommand);
        } catch (Exception e) {
            log.error("Build verification failed for service {}", config.getServiceName(), e);
            return new BuildResult(false,
                    "Build verification failed unexpectedly: " + e.getMessage(),
                    null,
                    defaultBuildCommand);
        }
    }

    private Path resolveWorkingDirectory(ServiceConfig config) throws IOException {
        String configuredPath = config.getWorkingDirectory();
        if (configuredPath == null || configuredPath.isBlank()) {
            return Path.of(baseGameServerPath, config.getServiceName()).normalize();
        }

        Path candidate = Path.of(configuredPath);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }

        return Path.of(baseGameServerPath).resolve(candidate).normalize();
    }

    public record BuildResult(
            boolean success,
            String message,
            String reportFile,
            String command
    ) {
    }
}
