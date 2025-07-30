package com.SouthMillion.user_service.enity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserItemEntity {
    @EmbeddedId
    private UserItemKey id;

    private int count;
}