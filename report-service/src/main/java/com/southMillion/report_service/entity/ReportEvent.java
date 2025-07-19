package com.southMillion.report_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "report_event")
@Data
public class ReportEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Các field chính
    private Integer type;
    private String agentId;
    private String deviceId;
    private String packageVersion;
    private String sourceVersion;
    private String sessionId;
    private Long loginTime;
    private Integer netState;
    private Long eventTime; // Thời điểm gửi event từ client
    private String imea;
    private String channelId;

    @Column(length = 2000)
    private String extraParams; // Nối các param mở rộng thành 1 chuỗi

    private Instant createdAt = Instant.now(); // Thời điểm lưu DB
}