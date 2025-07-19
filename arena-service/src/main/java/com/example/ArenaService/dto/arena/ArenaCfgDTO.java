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
public class ArenaCfgDTO {
    @JsonProperty("initial_num")
    private String initialNum;
    @JsonProperty("award_level_min")
    private String awardLevelMin;
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
    @JsonProperty("win_score_new")
    private String winScoreNew;
    @JsonProperty("lose_score_new")
    private String loseScoreNew;
    @JsonProperty("re_win_score_new")
    private String reWinScoreNew;
    @JsonProperty("re_lose_score_new")
    private String reLoseScoreNew;
    @JsonProperty("challenge_left_max")
    private String challengeLeftMax;
    @JsonProperty("challenge_left_min")
    private String challengeLeftMin;
    @JsonProperty("fixation_percent_left")
    private String fixationPercentLeft;
    @JsonProperty("left_score")
    private String leftScore;
    @JsonProperty("challenge_centre_max")
    private String challengeCentreMax;
    @JsonProperty("challenge_centre_min")
    private String challengeCentreMin;
    @JsonProperty("fixation_percent_centre")
    private String fixationPercentCentre;
    @JsonProperty("centre_score")
    private String centreScore;
    @JsonProperty("challenge_right_max")
    private String challengeRightMax;
    @JsonProperty("challenge_right_min")
    private String challengeRightMin;
    @JsonProperty("fixation_percent_right")
    private String fixationPercentRight;
    @JsonProperty("right_score")
    private String rightScore;
    @JsonProperty("constant")
    private String constant;
    @JsonProperty("int_min")
    private String intMin;
    @JsonProperty("int_max")
    private String intMax;
    @JsonProperty("arena_challenge_id")
    private String arenaChallengeId;
    @JsonProperty("succ")
    private List<ItemDTO> succ;
    @JsonProperty("lose")
    private List<ItemDTO> lose;
    @JsonProperty("player_min")
    private String playerMin;
    @JsonProperty("lose_score")
    private String loseScore;
    @JsonProperty("lose_score_min")
    private String loseScoreMin;
    @JsonProperty("lose_score_max")
    private String loseScoreMax;
    @JsonProperty("succ_score_min")
    private String succScoreMin;
    @JsonProperty("succ_score_max")
    private String succScoreMax;
    @JsonProperty("challenge_min")
    private String challengeMin;
    @JsonProperty("challenge_max")
    private String challengeMax;
    @JsonProperty("gold_expend")
    private String goldExpend;
    // getter/setter
}
