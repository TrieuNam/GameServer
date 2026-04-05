package com.SouthMillion.admin.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API view for the Service Doctor dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSessionView {
    private String serviceName;
    private String displayName;
    private String phase;
    private Integer port;
    private String serviceStatus;
    private String doctorStatus;
    private boolean watched;
    private boolean approvalRequired;
    private boolean autoApprovalEnabled;
    private boolean copilotReady;
    private String lastErrorType;
    private String lastErrorSummary;
    private String decisionNote;
    private String promptFile;
    private String reportFile;
    private String lastCommand;
    private List<String> recentLogs;
    private LocalDateTime lastUpdated;
}
