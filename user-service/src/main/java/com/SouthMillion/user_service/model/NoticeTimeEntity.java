package com.SouthMillion.user_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notice_time")
@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
public class NoticeTimeEntity {
    @Id
    private Long userId;
    private Long noticeTime;
    // getter/setter
}