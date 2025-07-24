package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_user")
@Setter
@Getter
public class NoticeUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userKey;
    private Long noticeId;
    private Boolean isRead;
    private LocalDateTime readTime;
    // Getter/Setter...
}
