package com.SouthMillion.globalserver_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mail")
@Getter
@Setter
public class MailEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long userId;
    private Integer mailType;
    private String subject;
    private String content;
    private Boolean isRead;
    private Boolean isFetched;
    private Long recvTime;
    private String itemDataJson; // Danh sách item/part thưởng, json string (list)

    // getters, setters
}
