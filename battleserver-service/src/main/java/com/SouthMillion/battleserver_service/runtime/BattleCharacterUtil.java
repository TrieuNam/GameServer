package com.SouthMillion.battleserver_service.runtime;

import org.SouthMillion.dto.battle.BattleRequestDTO;
import org.SouthMillion.dto.battle.MonsterInstanceDTO;
import org.SouthMillion.dto.battle.PassiveSkillDTO;
import org.SouthMillion.dto.battle.PetInstanceDTO;

import java.util.ArrayList;
import java.util.List;

public class BattleCharacterUtil {
    // PlayerDTO -> BattleCharacter
    public static BattleCharacter fromPlayerDTO(BattleRequestDTO req, List<PassiveSkillDTO> passiveConfig) {
        BattleCharacter c = new BattleCharacter();
        c.setInstanceId("player-" + req.getPlayerId());
        c.setName(req.getPlayerId());
        c.setAttack(req.getPlayerAttack());
        c.setDefense(req.getPlayerDefense());
        c.setSpeed(req.getPlayerSpeed());
        // Player máu = atk * 10 (hoặc tùy config)
        c.setHp(req.getPlayerAttack() * 10);
        c.setMaxHp(c.getHp());
        c.setSide("player");
        c.setSkillIds(req.getPlayerSkillIds() != null ? req.getPlayerSkillIds() : new ArrayList<>());
        c.setPassiveSkillIds(req.getPlayerPassiveSkillIds() != null ? req.getPlayerPassiveSkillIds() : new ArrayList<>());
        c.setActiveBuffs(new ArrayList<>());
        c.setState("normal");
        c.setAlive(true);
        c.applyPassive(passiveConfig);
        return c;
    }

    // Nếu bạn có PetInstanceDTO, bổ sung tương tự:
    public static BattleCharacter fromPetInstanceDTO(PetInstanceDTO pet, List<PassiveSkillDTO> passiveConfig) {
        BattleCharacter c = new BattleCharacter();
        c.setInstanceId("pet-" + pet.getInstanceId());
        c.setName(pet.getName());
        c.setAttack(pet.getAttack());
        c.setDefense(pet.getDefense());
        c.setSpeed(pet.getSpeed());
        c.setHp(pet.getHp());
        c.setMaxHp(pet.getHp());
        c.setSide("pet");
        c.setSkillIds(pet.getSkillIds() != null ? pet.getSkillIds() : new ArrayList<>());
        c.setPassiveSkillIds(pet.getPassiveSkillIds() != null ? pet.getPassiveSkillIds() : new ArrayList<>());
        c.setActiveBuffs(new ArrayList<>());
        c.setState("normal");
        c.setAlive(true);
        c.applyPassive(passiveConfig);
        return c;
    }

    // MonsterInstanceDTO -> BattleCharacter
    public static BattleCharacter fromMonsterDTO(MonsterInstanceDTO mi, List<PassiveSkillDTO> passiveConfig) {
        BattleCharacter c = new BattleCharacter();
        c.setInstanceId(mi.getInstanceId());
        c.setName(mi.getMonster() != null ? mi.getMonster().getName() : "monster");
        c.setAttack(mi.getAttack());
        c.setDefense(mi.getDefense());
        c.setSpeed(mi.getSpeed());
        c.setHp(mi.getHp());
        c.setMaxHp(mi.getHp());
        c.setSide("monster");
        // Nếu monster có skillIds/passiveSkillIds null, chuyển về empty list cho an toàn
        c.setSkillIds(mi.getSkillIds() != null ? mi.getSkillIds() : new ArrayList<>());
        c.setPassiveSkillIds(mi.getPassiveSkillIds() != null ? mi.getPassiveSkillIds() : new ArrayList<>());
        c.setActiveBuffs(mi.getActiveBuffs() != null ? mi.getActiveBuffs() : new ArrayList<>());
        c.setState(mi.getState() != null ? mi.getState() : "normal");
        c.setAlive(mi.isAlive());
        c.applyPassive(passiveConfig);
        return c;
    }
}
