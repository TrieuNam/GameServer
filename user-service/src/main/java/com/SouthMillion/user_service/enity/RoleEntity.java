package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role")
@Getter
@Setter
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long uid;  // User id

    @Column(nullable = false, unique = true, length = 64)
    private String username; // Account name (unique)

    @Column(nullable = false, length = 128)
    private String password; // Password (hashed)

    private Long curExp;
    private Long createTime;

    @Column(columnDefinition = "TEXT")
    private String roleinfoJson;

    @Column(columnDefinition = "TEXT")
    private String appearanceJson;

    @Column(columnDefinition = "TEXT")
    private String attrListJson;

    @Column(columnDefinition = "TEXT")
    private String petListJson;

    @Column(columnDefinition = "TEXT")
    private String equipListJson;

    @Column(columnDefinition = "TEXT")
    private String systemSetJson;

    private Long noticeTime;

    @Column(columnDefinition = "TEXT")
    private String coreLevelJson;

    private Integer angelAppearance;
    private Integer starMapLevel;
    private Integer gemLevel;
}