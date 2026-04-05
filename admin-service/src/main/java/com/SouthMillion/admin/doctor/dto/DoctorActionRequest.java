package com.SouthMillion.admin.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Optional request payload for doctor actions and settings updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorActionRequest {
    private String notes;
    private Boolean enabled;
    private String errorType;
    private String errorSummary;
    private List<String> errorLogs;
}
