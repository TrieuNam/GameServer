package com.SouthMillion.arenaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleRequest {
    private String playerId;
    private String opponentId;       // Optional — challenge mode
    /** Fight power of attacker (sum of HP+ATK+DEF+SPD from role-service).
     *  0 = not provided → fall back to rating-only formula */
    private long attackerPower;
    /** Fight power of defender. 0 = not provided */
    private long defenderPower;
}
