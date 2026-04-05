package com.SouthMillion.admin.doctor.service;

import com.SouthMillion.admin.doctor.dto.DoctorSessionView;
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
import java.util.Locale;

/**
 * Prepares Copilot repair prompts and optionally executes a configured local CLI command.
 */
@Service
@Slf4j
public class CopilotCliService {

    @Value("${doctor.copilot.enabled:false}")
    private boolean copilotEnabled;

    @Value("${doctor.copilot.command-template:}")
    private String commandTemplate;

    public CopilotResult prepareFix(ServiceConfig config, DoctorSessionView session, Path reportDirectory) {
        try {
            Files.createDirectories(reportDirectory);

            Path promptFile = reportDirectory.resolve(config.getServiceName() + "-copilot-prompt.md");
            Files.writeString(promptFile, buildPrompt(config, session), StandardCharsets.UTF_8);

            if (!copilotEnabled) {
                return new CopilotResult(
                        false,
                        false,
                        "Prompt prepared. Set `doctor.copilot.enabled=true` after installing GitHub CLI to execute automatically.",
                        promptFile.toString(),
                        null,
                        null
                );
            }

            String detectedCommand = detectCliCommand(commandTemplate);
            if (detectedCommand != null && !isCommandAvailable(detectedCommand)) {
                return new CopilotResult(
                        false,
                        false,
                        "Required CLI (`" + detectedCommand + "`) is not installed on this machine. Prompt file was prepared for manual use.",
                        promptFile.toString(),
                        null,
                        null
                );
            }

            if (commandTemplate == null || commandTemplate.isBlank()) {
                return new CopilotResult(
                        false,
                        false,
                        "No `doctor.copilot.command-template` is configured yet. Prompt file was prepared only.",
                        promptFile.toString(),
                        null,
                        null
                );
            }

            String command = commandTemplate
                    .replace("{promptFile}", promptFile.toString())
                    .replace("{serviceName}", config.getServiceName());

            CommandRunResult commandResult = runPowerShellCommand(command, reportDirectory);
            Path outputFile = reportDirectory.resolve(config.getServiceName() + "-copilot-output.txt");
            Files.writeString(outputFile, commandResult.output(), StandardCharsets.UTF_8);

            boolean success = commandResult.exitCode() == 0;
            String message = success
                    ? "Copilot CLI command executed. Review the output and diff before applying any patch."
                    : "Copilot CLI command failed. See the generated output file for details.";

            return new CopilotResult(
                    success,
                    true,
                    message,
                    promptFile.toString(),
                    outputFile.toString(),
                    command
            );
        } catch (Exception e) {
            log.error("Failed to prepare Copilot repair flow for service {}", config.getServiceName(), e);
            return new CopilotResult(false, false,
                    "Failed to prepare Copilot repair flow: " + e.getMessage(),
                    null, null, null);
        }
    }

    private String buildPrompt(ServiceConfig config, DoctorSessionView session) {
        String logs = session.getRecentLogs() == null || session.getRecentLogs().isEmpty()
                ? "No recent logs captured yet."
                : String.join(System.lineSeparator(), session.getRecentLogs());

        return "# Service Doctor Copilot Prompt" + System.lineSeparator()
                + System.lineSeparator()
                + "Service: " + config.getServiceName() + System.lineSeparator()
                + "Display name: " + safe(session.getDisplayName()) + System.lineSeparator()
                + "Phase: " + safe(session.getPhase()) + System.lineSeparator()
                + "Port: " + safe(session.getPort()) + System.lineSeparator()
                + "Current service status: " + safe(session.getServiceStatus()) + System.lineSeparator()
                + "Current doctor status: " + safe(session.getDoctorStatus()) + System.lineSeparator()
                + "Detected error type: " + safe(session.getLastErrorType()) + System.lineSeparator()
                + "Summary: " + safe(session.getLastErrorSummary()) + System.lineSeparator()
                + System.lineSeparator()
                + "## Recent logs" + System.lineSeparator()
                + "```text" + System.lineSeparator()
                + logs + System.lineSeparator()
                + "```" + System.lineSeparator()
                + System.lineSeparator()
                + "## Task" + System.lineSeparator()
                + "- find the root cause" + System.lineSeparator()
                + "- propose the smallest safe fix" + System.lineSeparator()
                + "- do not change unrelated files" + System.lineSeparator()
                + "- keep current behavior intact" + System.lineSeparator()
                + "- ensure the relevant Maven build passes after the fix" + System.lineSeparator();
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String detectCliCommand(String template) {
        if (template == null || template.isBlank()) {
            return null;
        }

        String normalized = template.toLowerCase(Locale.ROOT);
        if (normalized.contains("gh copilot")) {
            return "gh";
        }
        if (normalized.matches(".*(^|[^a-z])copilot([^a-z]|$).*")) {
            return "copilot";
        }
        return null;
    }

    private boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder("where.exe", command)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private CommandRunResult runPowerShellCommand(String command, Path workingDirectory) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", command)
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
        return new CommandRunResult(exitCode, output.toString());
    }

    private record CommandRunResult(int exitCode, String output) {
    }

    public record CopilotResult(
            boolean success,
            boolean executed,
            String message,
            String promptFile,
            String outputFile,
            String command
    ) {
    }
}
