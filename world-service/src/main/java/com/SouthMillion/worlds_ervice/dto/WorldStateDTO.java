package com.SouthMillion.worlds_ervice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldStateDTO {
    private String stateKey;
    private Map<String, Object> stateData;
    private Integer version;
    private Instant serverTime;
}
