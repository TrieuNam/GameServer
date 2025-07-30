package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "login")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_id")
    private Long loginId;

    @Column(name = "plat_user_name", nullable = false, unique = true, columnDefinition = "varbinary(64)")
    private String platUserName;

    @Column(name = "db_index")
    private Integer dbIndex; // có thể dùng để xác định cụm/shard

    @Column(name = "role_id_1")
    private Integer roleId1;

    @Column(name = "role_id_2")
    private Integer roleId2;

    @Column(name = "role_id_3")
    private Integer roleId3;

    @Column(name = "lastlogintime")
    private Long lastLoginTime;

    @Column(name = "createtime")
    private Long createTime;

    @Column(name = "forbidtime")
    private Long forbidTime;

    @Column(name = "is_anti_wallow")
    private Byte isAntiWallow;

    @Column(name = "token", length = 128)
    private String token; // nếu bạn có dùng token đăng nhập
}