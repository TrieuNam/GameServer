package org.SouthMillion.dto.report;

import lombok.Data;

import java.util.List;

@Data
public class ReportEventDTO {
    private Integer type;
    private String agentId;
    private String deviceId;
    private String packageVersion;
    private String sourceVersion;
    private String sessionId;
    private Long loginTime;
    private Integer netState;
    private Long eventTime;
    private String imea;
    private String channelId;
    private List<String> extraParams;
}