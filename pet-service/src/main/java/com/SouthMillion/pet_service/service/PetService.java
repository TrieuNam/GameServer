package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.SouthMillion.dto.pet.*;
import org.SouthMillion.dto.pet.Enum.PetBuffType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

@Service
public class PetService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    private final Map<Integer, PetDTO> petMap = new HashMap<>();
    private final Map<Integer, PetItemDTO> petItemMap = new HashMap<>();
    private final Map<Integer, PetWeaponDTO> petWeaponMap = new HashMap<>();
    private final List<PetUpDTO> petUpList = new ArrayList<>();
    private final List<PetAdvanceDTO> petAdvanceList = new ArrayList<>();
    private final List<PetSkillDTO> petSkillList = new ArrayList<>();

    @PostConstruct
    public void loadConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode petRoot = configFeignClient.getConfig("pet");
        List<PetDTO> pets = mapper.readValue(petRoot.get("pet").traverse(), new TypeReference<List<PetDTO>>() {});
        for (PetDTO p : pets) petMap.put(p.getPetId(), p);

        petUpList.addAll(mapper.readValue(petRoot.get("pet_up").traverse(), new TypeReference<List<PetUpDTO>>() {}));
        petAdvanceList.addAll(mapper.readValue(petRoot.get("pet_advance").traverse(), new TypeReference<List<PetAdvanceDTO>>() {}));
        petSkillList.addAll(mapper.readValue(petRoot.get("pet_skill").traverse(), new TypeReference<List<PetSkillDTO>>() {}));

        JsonNode itemRoot = configFeignClient.getConfig("pet_item");
        List<PetItemDTO> items = mapper.readValue(itemRoot.get("pet").traverse(), new TypeReference<List<PetItemDTO>>() {});
        for (PetItemDTO i : items) petItemMap.put(i.getId(), i);

        JsonNode weaponRoot = configFeignClient.getConfig("pet_weapon_item");
        List<PetWeaponDTO> weapons = mapper.readValue(weaponRoot.get("pet_weapon").traverse(), new TypeReference<List<PetWeaponDTO>>() {});
        for (PetWeaponDTO w : weapons) petWeaponMap.put(w.getId(), w);
    }

    public PetDTO getPetById(int id) { return petMap.get(id); }
    public List<PetDTO> getAllPets() { return new ArrayList<>(petMap.values()); }
    public PetItemDTO getPetItemById(int id) { return petItemMap.get(id); }
    public List<PetItemDTO> getAllPetItems() { return new ArrayList<>(petItemMap.values()); }
    public PetWeaponDTO getPetWeaponById(int id) { return petWeaponMap.get(id); }
    public List<PetWeaponDTO> getAllPetWeapons() { return new ArrayList<>(petWeaponMap.values()); }

    // --- Logic nâng cấp, advance, buff, ghép đá, random skill ---
    public boolean levelUpPet(int petId, int exp) {
        PetDTO pet = getPetById(petId);
        int curLevel = pet.getSkillGridMax();
        PetUpDTO config = petUpList.stream().filter(up -> up.getPetType() == pet.getPetType() && up.getPetLevel() == curLevel).findFirst().orElse(null);
        if (config == null) return false;
        // Simple exp logic
        if (exp < config.getUpExp()) return false; // thiếu exp
        pet.setSkillGridMax(curLevel + 1); // tăng level
        // cộng buff random
        PetBuff buff = new PetBuff();
        buff.setType(PetBuffType.CRIT);
        buff.setValue(5);
        buff.setTurns(10);
        pet.getBuffs().add(buff);
        // cộng up_att nếu có
        if (config.getUpAtt() != null) for (PetAttDTO add : config.getUpAtt()) addPetAttr(pet, add.getType(), add.getAdd());
        return true;
    }

    public boolean advancePet(int petId) {
        PetDTO pet = getPetById(petId);
        int order = pet.getPetOrder();
        PetAdvanceDTO advance = petAdvanceList.stream().filter(a -> a.getPetId() == petId && a.getPetOrder() == order + 1).findFirst().orElse(null);
        if (advance == null) return false;
        pet.setPetOrder(order + 1);
        if (advance.getUpAtt() != null) for (PetAttDTO att : advance.getUpAtt()) addPetAttr(pet, att.getType(), att.getAdd());
        if (advance.getUnlockSkillId() != null) pet.getSkillIds().add(advance.getUnlockSkillId());
        return true;
    }

    private void addPetAttr(PetDTO pet, int type, int value) {
        for (PetAttDTO att : pet.getPetAtt()) {
            if (att.getType() == type) { att.setAdd(att.getAdd() + value); return; }
        }
        PetAttDTO newAtt = new PetAttDTO(); newAtt.setType(type); newAtt.setAdd(value); pet.getPetAtt().add(newAtt);
    }

    public void applyBuff(PetDTO pet, int skillId) {
        // Tạo buff giả lập
        PetBuff buff = new PetBuff(); buff.setType(PetBuffType.SPEED); buff.setValue(10); buff.setTurns(5);
        pet.getBuffs().add(buff);
    }

    public int rollRandomSkill(int petId) {
        List<PetSkillDTO> skillPool = petSkillList.stream().filter(s -> s.getSkillColor() == 4).toList();
        Random random = new Random();
        int idx = random.nextInt(skillPool.size());
        return skillPool.get(idx).getSkillId();
    }

    public void useSkill(PetDTO pet, int skillId, PetDTO target) {
        PetSkillDTO skill = petSkillList.stream().filter(s -> s.getSkillId() == skillId).findFirst().orElse(null);
        if (skill == null) return;
        Random rand = new Random();
        if (skill.getRandomEffects() != null) {
            for (SkillEffect effect : skill.getRandomEffects()) {
                if (rand.nextInt(100) < effect.getRate()) applyEffect(target, effect);
            }
        }
    }

    private void applyEffect(PetDTO target, SkillEffect effect) {
        if ("STUN".equals(effect.getEffectType())) target.setStunned(true);
        if ("DOUBLE_DAMAGE".equals(effect.getEffectType())) target.setNextDamageMultiplier(2.0);
    }

    public String fightBoss(PetDTO playerPet, PetDTO bossPet) {
        int hpPlayer = getStat(playerPet, 1), hpBoss = getStat(bossPet, 1);
        Random random = new Random();
        int round = 0;
        while (hpPlayer > 0 && hpBoss > 0 && round < 99) {
            hpBoss -= Math.max(1, getStat(playerPet, 2) - getStat(bossPet, 3) + random.nextInt(10));
            if (hpBoss <= 0) break;
            hpPlayer -= Math.max(1, getStat(bossPet, 2) - getStat(playerPet, 3) + random.nextInt(10));
            round++;
        }
        return hpPlayer > 0 ? "WIN" : "LOSE";
    }
    public String teamBattle(List<PetDTO> teamA, List<PetDTO> teamB) {
        int winA = 0, winB = 0;
        for (int i = 0; i < Math.min(teamA.size(), teamB.size()); i++) {
            String result = fightBoss(teamA.get(i), teamB.get(i));
            if ("WIN".equals(result)) winA++; else winB++;
        }
        return winA > winB ? "TeamA WIN" : (winB > winA ? "TeamB WIN" : "DRAW");
    }
    private int getStat(PetDTO pet, int type) {
        return pet.getPetAtt().stream().filter(a -> a.getType() == type).findFirst().map(PetAttDTO::getAdd).orElse(0);
    }

    public PetStatsDTO calcPetStats(int petId, Integer itemId, Integer weaponId) {
        PetDTO pet = getPetById(petId);
        int hp = getStat(pet, 1), atk = getStat(pet, 2), def = getStat(pet, 3), speed = getStat(pet, 4);
        if (itemId != null) { PetItemDTO item = getPetItemById(itemId); if (item != null) hp += item.getParam(); }
        if (weaponId != null) { PetWeaponDTO weapon = getPetWeaponById(weaponId); if (weapon != null) atk += weapon.getParam(); }
        return new PetStatsDTO(hp, atk, def, speed);
    }
}