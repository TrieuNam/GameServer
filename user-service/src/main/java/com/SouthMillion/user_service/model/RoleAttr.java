package com.SouthMillion.user_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_attrs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAttr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int attrType;
    private long attrValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;
}