package com.SouthMillion.main_fb_service.service.remote;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaoxianConfig {
    private List<Clearance> clearance;
    private List<JieduanReward> jieduan_reward;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Clearance {
        private String stage;
        private String level;
        private String score;           // 3-star score threshold
        private String now_box;         // Stamina cost for entering
        private String quick_num;       // Max sweep times
        private String quick_expend;    // Stamina cost for sweeping (e.g. "10|10|10")
        private List<Drop> win;         // Rewards for clearing
        private String unlock_req;      // Unlock requirement (optional)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JieduanReward {
        private String stage;
        private String chapter;
        private String clearance_condition; // Number of stages to clear
        private List<Drop> win;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Drop {
        private String item_id;
        private String num;
    }
}
