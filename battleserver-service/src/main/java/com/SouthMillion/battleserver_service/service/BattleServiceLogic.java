package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.runtime.BattleCharacter;
import com.SouthMillion.battleserver_service.runtime.BattleCharacterUtil;
import com.SouthMillion.battleserver_service.service.serviceClient.WorldServiceClient;
import com.SouthMillion.battleserver_service.service.skills.passiveSkills.PassiveSkillService;
import com.SouthMillion.battleserver_service.service.skills.singleSkills.SingleSkillService;
import jakarta.annotation.PostConstruct;
import org.SouthMillion.dto.battle.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BattleServiceLogic {

    @Autowired
    SingleSkillService singleSkillService;

    @Autowired
    PassiveSkillService passiveSkillService;

    @Autowired
    WorldServiceClient worldFeign;

    private List<PassiveSkillDTO> passives;
    private List<SingleSkillDTO> skills;
    private List<SingleSkillEffectDTO> skillEffects;

    @PostConstruct
    public void loadConfig() {
        passives = passiveSkillService.getAllPassiveCfg();
        skills = singleSkillService.getAllSkillCfg();
        skillEffects = singleSkillService.getAllSkillEff();
    }

    public BattleResultDTO doBattle(BattleRequestDTO req) {
        // 1. Build runtime object cho player, pet, monster (side đầy đủ)
        List<BattleCharacter> allChars = new ArrayList<>();
        BattleCharacter playerChar = BattleCharacterUtil.fromPlayerDTO(req, passives);
        allChars.add(playerChar);

        if (req.getPetInstances() != null) {
            for (PetInstanceDTO pet : req.getPetInstances()) {
                allChars.add(BattleCharacterUtil.fromPetInstanceDTO(pet, passives));
            }
        }

        for (MonsterInstanceDTO mi : req.getMonsters()) {
            allChars.add(BattleCharacterUtil.fromMonsterDTO(mi, passives));
        }

        // 2. Tick buff, xử lý passive skill, cộng chỉ số đầu trận
        for (BattleCharacter c : allChars) c.applyPassive(passives);

        // 3. Multi-round combat
        final int MAX_ROUND = 20;
        List<BattleStepDTO> battleSteps = new ArrayList<>();
        for (int round = 1; round <= MAX_ROUND; round++) {
            // Tick buff đầu mỗi round
            for (BattleCharacter c : allChars) if (c.isAlive()) c.tickBuffs();

            // Sắp xếp theo speed giảm dần
            List<BattleCharacter> actionOrder = allChars.stream()
                    .filter(BattleCharacter::isAlive)
                    .sorted(Comparator.comparingInt(BattleCharacter::getSpeed).reversed())
                    .collect(Collectors.toList());

            List<ActionDTO> roundActions = new ArrayList<>();
            for (BattleCharacter actor : actionOrder) {
                if (!actor.isAlive() || !actor.canAct()) continue; // chết hoặc bị stun, skip

                // Chọn target(s)
                int targetNum = 1;
                SkillUseInfo skillToUse = selectSkill(actor, skills, skillEffects);
                if (skillToUse != null && skillToUse.getSkillDTO() != null) {
                    try { targetNum = Integer.parseInt(skillToUse.getSkillDTO().getTargetNum()); }
                    catch (Exception ignore) {}
                }
                List<BattleCharacter> targets = pickTargets(actor, allChars, targetNum);
                if (targets.isEmpty()) continue;
                for (BattleCharacter target : targets) {
                    ActionDTO act;
                    if (skillToUse != null) {
                        act = useSkill(actor, target, skillToUse, allChars);
                    } else {
                        act = basicAttack(actor, target);
                    }
                    roundActions.add(act);
                }
            }
            battleSteps.add(new BattleStepDTO(round, roundActions));

            // Check win/lose
            boolean playerSideAlive = allChars.stream().anyMatch(BattleCharacter::isPlayerSideAndAlive);
            boolean monsterSideAlive = allChars.stream().anyMatch(BattleCharacter::isMonsterSideAndAlive);
            if (!playerSideAlive || !monsterSideAlive) break;
        }

        boolean playerSideAlive = allChars.stream().anyMatch(BattleCharacter::isPlayerSideAndAlive);

        // Gọi lại world-service nếu monster die
        allChars.stream().filter(BattleCharacter::isMonsterSide)
                .filter(c -> !c.isAlive())
                .forEach(monster -> {
                    MonsterKilledRequestDTO killedReq = new MonsterKilledRequestDTO();
                    killedReq.setMonsterInstanceId(monster.getInstanceId());
                    worldFeign.monsterKilled(killedReq);
                });

        BattleResultDTO result = new BattleResultDTO();
        result.setWin(playerSideAlive);
        result.setBattleSteps(battleSteps);

        // MonsterInstanceDTO cuối trận
        List<MonsterInstanceDTO> monsters = allChars.stream()
                .filter(BattleCharacter::isMonsterSide)
                .map(BattleCharacter::toMonsterInstanceDTO)
                .collect(Collectors.toList());
        result.setMonstersAfterBattle(monsters);
        return result;
    }

    // pickTargets: chọn N target đúng side đối diện, random hoặc theo máu thấp nhất
    private List<BattleCharacter> pickTargets(BattleCharacter actor, List<BattleCharacter> all, int targetNum) {
        List<BattleCharacter> pool;
        if (actor.isMonsterSide()) {
            pool = all.stream().filter(BattleCharacter::isPlayerSideAndAlive).collect(Collectors.toList());
        } else {
            pool = all.stream().filter(BattleCharacter::isMonsterSideAndAlive).sorted(Comparator.comparingInt(BattleCharacter::getHp)).collect(Collectors.toList());
        }
        Collections.shuffle(pool);
        return pool.stream().limit(targetNum).collect(Collectors.toList());
    }

    // ... toàn bộ code selectSkill, useSkill, basicAttack, calcSkillDmg đã gửi ở trên

    // Chọn skill: ưu tiên skill stun, không có thì random skill, không có thì đánh thường
    private SkillUseInfo selectSkill(
            BattleCharacter actor,
            List<SingleSkillDTO> skillConfig,
            List<SingleSkillEffectDTO> effectConfig
    ) {
        List<String> skillIds = actor.getSkillIds();
        if (skillIds == null || skillIds.isEmpty()) return null;
        for (String skillId : skillIds) {
            SingleSkillDTO skill = skillConfig.stream()
                    .filter(s -> s.getSkillId().equals(skillId)).findFirst().orElse(null);
            if (skill == null) continue;
            List<SingleSkillEffectDTO> effects = getSkillEffects(skill, effectConfig);
            boolean hasStun = effects.stream().anyMatch(e -> "stun".equals(mapEffectTypeName(e.getEffectType())));
            if (hasStun) {
                SkillUseInfo info = new SkillUseInfo();
                info.setSkillId(skillId);
                info.setSkillDTO(skill);
                info.setEffects(effects);
                return info;
            }
        }
        String skillId = skillIds.get(new Random().nextInt(skillIds.size()));
        SingleSkillDTO skill = skillConfig.stream()
                .filter(s -> s.getSkillId().equals(skillId)).findFirst().orElse(null);
        if (skill == null) return null;
        SkillUseInfo info = new SkillUseInfo();
        info.setSkillId(skillId);
        info.setSkillDTO(skill);
        info.setEffects(getSkillEffects(skill, effectConfig));
        return info;
    }

    private List<SingleSkillEffectDTO> getSkillEffects(SingleSkillDTO skill, List<SingleSkillEffectDTO> effectConfig) {
        List<SingleSkillEffectDTO> result = new ArrayList<>();
        if (skill.getEffect1() != null && !skill.getEffect1().isEmpty())
            result.add(effectConfig.stream().filter(e -> e.getSeq().equals(skill.getEffect1())).findFirst().orElse(null));
        if (skill.getEffect2() != null && !skill.getEffect2().isEmpty())
            result.add(effectConfig.stream().filter(e -> e.getSeq().equals(skill.getEffect2())).findFirst().orElse(null));
        if (skill.getEffect3() != null && !skill.getEffect3().isEmpty())
            result.add(effectConfig.stream().filter(e -> e.getSeq().equals(skill.getEffect3())).findFirst().orElse(null));
        if (skill.getEffect4() != null && !skill.getEffect4().isEmpty())
            result.add(effectConfig.stream().filter(e -> e.getSeq().equals(skill.getEffect4())).findFirst().orElse(null));
        result.removeIf(Objects::isNull);
        return result;
    }

    private String mapEffectTypeName(String effectType) {
        switch (effectType) {
            case "1": return "damage";
            case "2": return "stun";
            case "3": return "heal";
            case "4": return "shield";
            case "5": return "poison";
            case "6": return "reflect";
            case "7": return "immune";
            default: return "damage";
        }
    }

    // Xử lý dùng skill
    private ActionDTO useSkill(
            BattleCharacter actor,
            BattleCharacter target,
            SkillUseInfo skillToUse,
            List<BattleCharacter> allChars
    ) {
        ActionDTO act = new ActionDTO();
        act.setActorId(actor.getInstanceId());
        act.setSkillId(skillToUse.getSkillId());
        act.setTargetId(target.getInstanceId());
        act.setBuffsApplied(new ArrayList<>());

        int totalDmg = 0;
        StringBuilder effectLog = new StringBuilder();

        for (SingleSkillEffectDTO effect : skillToUse.getEffects()) {
            String effType = mapEffectTypeName(effect.getEffectType());
            switch (effType) {
                case "damage":
                    int dmg = calcSkillDmg(actor, target, effect);
                    totalDmg += target.takeDamage(dmg, actor);
                    effectLog.append("damage ");
                    break;
                case "stun":
                    BuffStatusDTO stunBuff = new BuffStatusDTO();
                    stunBuff.setBuffId("stun-" + UUID.randomUUID());
                    stunBuff.setName("Stun");
                    stunBuff.setEffectType("stun");
                    stunBuff.setDuration(1);
                    stunBuff.setFrom(actor.getInstanceId());
                    target.applyBuff(stunBuff);
                    act.getBuffsApplied().add(stunBuff);
                    effectLog.append("stun ");
                    break;
                case "heal":
                    int healVal = Integer.parseInt(effect.getParam1());
                    actor.heal(healVal);
                    effectLog.append("heal ");
                    break;
                case "shield":
                    BuffStatusDTO shieldBuff = new BuffStatusDTO();
                    shieldBuff.setBuffId("shield-" + UUID.randomUUID());
                    shieldBuff.setName("Shield");
                    shieldBuff.setEffectType("shield");
                    shieldBuff.setDuration(Integer.parseInt(effect.getParam2())); // param2: số round
                    shieldBuff.setValue(Integer.parseInt(effect.getParam1())); // param1: máu absorb
                    shieldBuff.setFrom(actor.getInstanceId());
                    actor.applyBuff(shieldBuff);
                    act.getBuffsApplied().add(shieldBuff);
                    effectLog.append("shield ");
                    break;
                case "poison":
                    BuffStatusDTO poisonBuff = new BuffStatusDTO();
                    poisonBuff.setBuffId("poison-" + UUID.randomUUID());
                    poisonBuff.setName("Poison");
                    poisonBuff.setEffectType("poison");
                    poisonBuff.setDuration(Integer.parseInt(effect.getParam2()));
                    poisonBuff.setValue(Integer.parseInt(effect.getParam1()));
                    poisonBuff.setFrom(actor.getInstanceId());
                    target.applyBuff(poisonBuff);
                    act.getBuffsApplied().add(poisonBuff);
                    effectLog.append("poison ");
                    break;
                case "reflect":
                    BuffStatusDTO reflectBuff = new BuffStatusDTO();
                    reflectBuff.setBuffId("reflect-" + UUID.randomUUID());
                    reflectBuff.setName("Reflect");
                    reflectBuff.setEffectType("reflect");
                    reflectBuff.setDuration(Integer.parseInt(effect.getParam2()));
                    reflectBuff.setValue(Integer.parseInt(effect.getParam1()));
                    reflectBuff.setFrom(actor.getInstanceId());
                    actor.applyBuff(reflectBuff);
                    act.getBuffsApplied().add(reflectBuff);
                    effectLog.append("reflect ");
                    break;
                case "immune":
                    BuffStatusDTO immuneBuff = new BuffStatusDTO();
                    immuneBuff.setBuffId("immune-" + UUID.randomUUID());
                    immuneBuff.setName("Immune");
                    immuneBuff.setEffectType("immune");
                    immuneBuff.setDuration(Integer.parseInt(effect.getParam2()));
                    immuneBuff.setFrom(actor.getInstanceId());
                    actor.applyBuff(immuneBuff);
                    act.getBuffsApplied().add(immuneBuff);
                    effectLog.append("immune ");
                    break;
                default:
                    break;
            }
        }
        act.setDamage(totalDmg);
        act.setEffect(effectLog.toString().trim());
        act.setDead(!target.isAlive());
        return act;
    }

    // Đánh thường (không dùng skill)
    private ActionDTO basicAttack(BattleCharacter actor, BattleCharacter target) {
        ActionDTO act = new ActionDTO();
        act.setActorId(actor.getInstanceId());
        act.setSkillId("BASIC_ATTACK");
        act.setTargetId(target.getInstanceId());
        int baseDmg = Math.max(0, actor.getAttack() - target.getDefense());
        int realDmg = target.takeDamage(baseDmg, actor);
        act.setDamage(realDmg);
        act.setEffect("damage");
        act.setDead(!target.isAlive());
        return act;
    }

    // Hàm tính damage cho skill
    private int calcSkillDmg(BattleCharacter actor, BattleCharacter target, SingleSkillEffectDTO effect) {
        int base = 0;
        try { base = Integer.parseInt(effect.getParam1()); } catch (Exception ignore) {}
        return Math.max(0, base + actor.getAttack() - target.getDefense());
    }
}

