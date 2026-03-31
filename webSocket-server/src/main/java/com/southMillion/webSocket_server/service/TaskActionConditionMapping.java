package com.SouthMillion.webSocket_server.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.task-progress.conditions")
public class TaskActionConditionMapping {

    private Integer guildJoin;
    private Integer guildCreate;
    private Integer arenaWin;
    private Integer trialComplete;
    private Integer petActivate;
    private Integer petUpgrade;
    private Integer petEvolve;
    private Integer mountLevelUp;
    private Integer mountGradeUp;
    private Integer mountWear;
    private Integer mountDecompose;
    private Integer skillLearn;
    private Integer skillOneKeyLevelUp;
    private Integer craftingStart;
    private Integer craftingClaim;
    private Integer gemInlay;
    private Integer gemCompose;
    private Integer gemUpgrade;
    private Integer gemBuy;
    private Integer shenqiActivate;
    private Integer shenqiUpgrade;
    private Integer shenqiEvolve;
    private Integer shenqiRefine;
    private Integer shenqiDraw;
    private Integer angelLevelUp;
    private Integer angelGradeUp;
    private Integer angelEquip;
    private Integer angelAppearanceLevelUp;
    private Integer runeLevelUp;
    private Integer runeUpgradeQuality;
    private Integer runeEquip;
    private Integer escortStart;
    private Integer escortComplete;
    private Integer escortRob;
    private Integer territoryFetchReward;
    private Integer territoryLevelUp;
    private Integer pagodaShilianChallenge;
    private Integer pagodaShilianClaim;
    private Integer pagodaGumoChallenge;
    private Integer pagodaGumoClaim;
    private Integer lingzhuChallenge;
    private Integer lingzhuSweep;
    private Integer shizhuangEquip;
    private Integer shizhuangFumo;
    private Integer blockInlay;
    private Integer blockRemove;
    private Integer blockCompose;
    private Integer formationLevelUp;

    public String guildJoinTaskKey() {
        return toConditionKey(guildJoin);
    }

    public String guildCreateTaskKey() {
        return toConditionKey(guildCreate);
    }

    public String arenaWinTaskKey() {
        return toConditionKey(arenaWin);
    }

    public String trialCompleteTaskKey() {
        return toConditionKey(trialComplete);
    }

    public String petActivateTaskKey() {
        return toConditionKey(petActivate);
    }

    public String petUpgradeTaskKey() {
        return toConditionKey(petUpgrade);
    }

    public String petEvolveTaskKey() {
        return toConditionKey(petEvolve);
    }

    public String mountLevelUpTaskKey() {
        return toConditionKey(mountLevelUp);
    }

    public String mountGradeUpTaskKey() {
        return toConditionKey(mountGradeUp);
    }

    public String mountWearTaskKey() {
        return toConditionKey(mountWear);
    }

    public String mountDecomposeTaskKey() {
        return toConditionKey(mountDecompose);
    }

    public String skillLearnTaskKey() {
        return toConditionKey(skillLearn);
    }

    public String skillOneKeyLevelUpTaskKey() {
        return toConditionKey(skillOneKeyLevelUp);
    }

    public String craftingStartTaskKey() {
        return toConditionKey(craftingStart);
    }

    public String craftingClaimTaskKey() {
        return toConditionKey(craftingClaim);
    }

    public String gemInlayTaskKey() {
        return toConditionKey(gemInlay);
    }

    public String gemComposeTaskKey() {
        return toConditionKey(gemCompose);
    }

    public String gemUpgradeTaskKey() {
        return toConditionKey(gemUpgrade);
    }

    public String gemBuyTaskKey() {
        return toConditionKey(gemBuy);
    }

    public String shenqiActivateTaskKey() {
        return toConditionKey(shenqiActivate);
    }

    public String shenqiUpgradeTaskKey() {
        return toConditionKey(shenqiUpgrade);
    }

    public String shenqiEvolveTaskKey() {
        return toConditionKey(shenqiEvolve);
    }

    public String shenqiRefineTaskKey() {
        return toConditionKey(shenqiRefine);
    }

    public String shenqiDrawTaskKey() {
        return toConditionKey(shenqiDraw);
    }

    public String angelLevelUpTaskKey() {
        return toConditionKey(angelLevelUp);
    }

    public String angelGradeUpTaskKey() {
        return toConditionKey(angelGradeUp);
    }

    public String angelEquipTaskKey() {
        return toConditionKey(angelEquip);
    }

    public String angelAppearanceLevelUpTaskKey() {
        return toConditionKey(angelAppearanceLevelUp);
    }

    public String runeLevelUpTaskKey() {
        return toConditionKey(runeLevelUp);
    }

    public String runeUpgradeQualityTaskKey() {
        return toConditionKey(runeUpgradeQuality);
    }

    public String runeEquipTaskKey() {
        return toConditionKey(runeEquip);
    }

    public String escortStartTaskKey() {
        return toConditionKey(escortStart);
    }

    public String escortCompleteTaskKey() {
        return toConditionKey(escortComplete);
    }

    public String escortRobTaskKey() {
        return toConditionKey(escortRob);
    }

    public String territoryFetchRewardTaskKey() {
        return toConditionKey(territoryFetchReward);
    }

    public String territoryLevelUpTaskKey() {
        return toConditionKey(territoryLevelUp);
    }

    public String pagodaShilianChallengeTaskKey() {
        return toConditionKey(pagodaShilianChallenge);
    }

    public String pagodaShilianClaimTaskKey() {
        return toConditionKey(pagodaShilianClaim);
    }

    public String pagodaGumoChallengeTaskKey() {
        return toConditionKey(pagodaGumoChallenge);
    }

    public String pagodaGumoClaimTaskKey() {
        return toConditionKey(pagodaGumoClaim);
    }

    public String lingzhuChallengeTaskKey() {
        return toConditionKey(lingzhuChallenge);
    }

    public String lingzhuSweepTaskKey() {
        return toConditionKey(lingzhuSweep);
    }

    public String shizhuangEquipTaskKey() {
        return toConditionKey(shizhuangEquip);
    }

    public String shizhuangFumoTaskKey() {
        return toConditionKey(shizhuangFumo);
    }

    public String blockInlayTaskKey() {
        return toConditionKey(blockInlay);
    }

    public String blockRemoveTaskKey() {
        return toConditionKey(blockRemove);
    }

    public String blockComposeTaskKey() {
        return toConditionKey(blockCompose);
    }

    public String formationLevelUpTaskKey() {
        return toConditionKey(formationLevelUp);
    }

    private String toConditionKey(Integer conditionId) {
        if (conditionId == null || conditionId <= 0) {
            return null;
        }
        return "condition_" + conditionId;
    }
}