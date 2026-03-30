package com.SouthMillion.worlds_ervice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldEventDTO {
    private Long eventId;
    private String eventName;
    private String eventType;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private Boolean active;
    private Boolean recurring;
    private Long remainingSeconds;
}
