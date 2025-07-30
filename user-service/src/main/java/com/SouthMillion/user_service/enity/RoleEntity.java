package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @Column(name = "role_id")
    private Integer roleId;  // Đây là PK nghiệp vụ, không phải auto_increment

    @Column(name = "role_inc_id")
    private Long roleIncId; // Nếu dùng để index nhanh (auto_increment DB)

    @Column(name = "role_name", columnDefinition = "varbinary(64)")
    private String roleName;

    @Column(name = "avatar_type")
    private Integer avatarType;

    @Column(name = "appearance_id")
    private Integer appearanceId;

    @Column(name = "scene_id")
    private Integer sceneId;

    @Column(name = "scene_x")
    private Integer sceneX;

    @Column(name = "scene_y")
    private Integer sceneY;

    @Column(name = "last_scene_id")
    private Integer lastSceneId;

    @Column(name = "last_scene_x")
    private Integer lastSceneX;

    @Column(name = "last_scene_y")
    private Integer lastSceneY;

    @Column(name = "level")
    private Integer level;

    @Column(name = "profession")
    private Integer profession;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "online_time")
    private Integer onlineTime;

    @Column(name = "is_online")
    private Byte isOnline;

    @Column(name = "last_save_time")
    private Long lastSaveTime;

    @Column(name = "plat_spid")
    private Integer platSpid;

    @Column(name = "forbid_time")
    private Long forbidTime;

    @Column(name = "plat_user_name", columnDefinition = "varbinary(64)")
    private String platUserName; // mapping với login

    @Column(name = "role_wx_head", columnDefinition = "varbinary(512)")
    private String roleWxHead;
}