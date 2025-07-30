package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_resource")
@Data
public class UserResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Integer itemId;
    private Integer amount;
}