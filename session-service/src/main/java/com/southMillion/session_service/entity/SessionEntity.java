package com.southMillion.session_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "session")
@Setter
@Getter
public class SessionEntity {
    @Id
    private Long userId;

    private Long lastHeartbeat;
    private Long lastTimeSync;

    // getters/setters
}