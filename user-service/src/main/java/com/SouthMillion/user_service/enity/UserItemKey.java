package com.SouthMillion.user_service.enity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserItemKey implements Serializable {
    private Long userId;
    private Integer itemId;
}