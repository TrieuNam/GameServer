package com.SouthMillion.gameworld_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyPlayersResponse {
    private Long requesterId;
    private Integer count;
    private List<PlayerPosition> players;
    private Long timestamp;
}
