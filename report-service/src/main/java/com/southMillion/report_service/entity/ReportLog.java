package com.southMillion.report_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String eventType;
    private String payloadJson;
    private long timestamp;
}
