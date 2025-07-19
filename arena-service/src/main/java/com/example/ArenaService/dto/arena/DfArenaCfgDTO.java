package com.example.ArenaService.dto.arena;

import com.example.ArenaService.dto.Item.ItemDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class DfArenaCfgDTO {
    @JsonProperty("initial_num")
    private String initialNum;
    @JsonProperty("award_level_min")
    private String awardLevelMin;
    @JsonProperty("daily_rank_num")
    private String dailyRankNum;
    @JsonProperty("weekly_rank_num")
    private String weeklyRankNum;
    @JsonProperty("combat_log_max")
    private String combatLogMax;
    @JsonProperty("refresh_interval_s")
    private String refreshIntervalS;
    @JsonProperty("everyday_time")
    private String everydayTime;
    @JsonProperty("jiesuan_tiem")
    private String jiesuanTiem;
    @JsonProperty("refresh_score")
    private String refreshScore;
    @JsonProperty("u_challenge_up_max")
    private String uChallengeUpMax;
    @JsonProperty("u_challenge_up_min")
    private String uChallengeUpMin;
    @JsonProperty("u_challenge_down_max")
    private String uChallengeDownMax;
    @JsonProperty("u_challeng_down_min")
    private String uChallengDownMin;
    @JsonProperty("s_challenge_up_max")
    private String sChallengeUpMax;
    @JsonProperty("s_challenge_up_min")
    private String sChallengeUpMin;
    @JsonProperty("s_challenge_down_max")
    private String sChallengeDownMax;
    @JsonProperty("s_challeng_down_min")
    private String sChallengDownMin;
    @JsonProperty("sarena_challenger_id")
    private String sarenaChallengerId;
    @JsonProperty("win_score")
    private String winScore;
    @JsonProperty("lose_score")
    private String loseScore;
    @JsonProperty("re_win_score")
    private String reWinScore;
    @JsonProperty("re_lose_score")
    private String reLoseScore;
    @JsonProperty("succ")
    private List<ItemDTO> succ;
    @JsonProperty("lose")
    private List<ItemDTO> lose;
    @JsonProperty("diamond_expend")
    private String diamondExpend;
    // getter/setter
}