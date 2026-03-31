package com.SouthMillion.admin.dto;

import com.SouthMillion.admin.entity.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceControlResponse {
    private String serviceName;
    private ServiceStatus status;
    private String message;
    private boolean success;
    private Long processId;
    private Integer port;
    private LocalDateTime timestamp;
}
