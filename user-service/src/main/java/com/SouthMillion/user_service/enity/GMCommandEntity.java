package com.SouthMillion.user_service.enity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gm_command_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GMCommandEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "command_type")
    private String commandType;

    @Column(name = "command")
    private String command;

    @Column(name = "result")
    private String result;

    @Column(name = "exec_time")
    private Long execTime;
}