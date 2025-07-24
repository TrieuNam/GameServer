package org.SouthMillion.dto.serverInfor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerTimeEventDto {
    private long serverTime;
    private long serverRealStartTime;
    private int openDays;
    private long serverRealCombineTime;
}