package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id")
    private Integer roleId;

    @Lob
    @Column(name = "system_set_list", columnDefinition = "text")
    private String systemSetListJson; // Lưu danh sách setting dạng JSON (serialize/de-serialize list PB_system_set)
}