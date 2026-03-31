package com.SouthMillion.admin.dto;

import com.SouthMillion.admin.entity.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {
    private Long id;
    private String serviceName;
    private String displayName;
    private String description;
    private int port;
    private String phase;
    private int startupOrder;
    private ServiceStatus status;
    private Long processId;
    private String healthCheckUrl;
    private boolean enabled;
    private boolean autoStart;
    private String containerName;
    private boolean requiresDocker;
    private String dockerStatus;
}
