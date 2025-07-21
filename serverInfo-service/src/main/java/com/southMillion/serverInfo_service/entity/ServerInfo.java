package com.southMillion.serverInfo_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "server_info")
@Data
public class ServerInfo {
    @Id
    private Integer id = 1; // Luôn chỉ có 1 bản ghi

    @Column(name = "real_start_time", nullable = false)
    private Long realStartTime;   // Unix timestamp (seconds)

    @Column(name = "real_combine_time", nullable = false)
    private Long realCombineTime = 0L; // Unix timestamp (seconds), 0 nếu chưa hợp

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;
}