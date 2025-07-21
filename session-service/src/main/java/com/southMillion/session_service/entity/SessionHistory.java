package com.southMillion.session_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "session_history")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class SessionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roleId;
    private String sessionId;
    private Instant loginTime;
    private Instant logoutTime;
}
