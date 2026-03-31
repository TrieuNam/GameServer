package com.SouthMillion.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStatsDTO {
    private Long totalReports;
    private Long resolvedReports;
    private Long pendingReports;
    private Long rejectedReports;
    private Double resolutionRate;
    private Long todayReports;
    private Long activeDevices;
}
