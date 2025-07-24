package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private Boolean isGlobal;
    // Getter/Setter...
}