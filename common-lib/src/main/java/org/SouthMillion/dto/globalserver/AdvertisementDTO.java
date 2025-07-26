package org.SouthMillion.dto.globalserver;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.proto.Msgother.Msgother;

@Data
public class AdvertisementDTO {
    private Integer seq;
    private Integer todayCount;
    private Integer nextFetchTime;


    public Msgother.PB_SCAdvertisement toProto() {
        return Msgother.PB_SCAdvertisement.newBuilder()
                .setSeq(seq == null ? 0 : seq)
                .setTodayCount(todayCount == null ? 0 : todayCount)
                .setNextFetchTime(nextFetchTime == null ? 0 : nextFetchTime)
                .build();
    }
}