package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.entity.PetClothEntity;
import com.SouthMillion.pet_service.entity.PetEntity;
import com.SouthMillion.pet_service.entity.PetTSGemEntity;
import com.SouthMillion.pet_service.repository.PetClothRepository;
import com.SouthMillion.pet_service.repository.PetRepository;
import com.SouthMillion.pet_service.repository.PetTSGemRepository;
import com.SouthMillion.pet_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PetService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    @Autowired
    private PetRepository petRepository;
    @Autowired
    private PetTSGemRepository tsGemRepository;
    @Autowired
    private PetClothRepository clothRepository;

    private final Map<Integer, PetConfigDTO.PetDTO> petMap = new HashMap<>();
    private final Map<Integer, PetItemDTO> petItemMap = new HashMap<>();
    private final Map<Integer, PetWeaponDTO> petWeaponMap = new HashMap<>();
    private final List<PetConfigDTO.PetUpDTO> petUpList = new ArrayList<>();
    private final List<PetConfigDTO.PetAdvanceDTO> petAdvanceList = new ArrayList<>();
    private final List<PetConfigDTO.PetSkillDTO> petSkillList = new ArrayList<>();

    @PostConstruct
    public void loadConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Load toàn bộ file pet.json vào DTO gốc
        JsonNode petRoot = configFeignClient.getConfig("pet");
        PetConfigDTO config = mapper.convertValue(petRoot, PetConfigDTO.class);

        for (PetConfigDTO.PetDTO p : config.getPet()) petMap.put(p.getPetId(), p);
        petUpList.addAll(config.getPetUpDTOS());
        petAdvanceList.addAll(config.getPetAdvanceDTO());
        petSkillList.addAll(config.getPetSkillDTO());

        // Pet item
        JsonNode itemRoot = configFeignClient.getConfig("pet_item");
        List<PetItemDTO> items = mapper.readValue(itemRoot.get("pet").traverse(), new TypeReference<List<PetItemDTO>>() {});
        for (PetItemDTO i : items) petItemMap.put(i.getId(), i);

        // Pet weapon
        JsonNode weaponRoot = configFeignClient.getConfig("pet_weapon_item");
        List<PetWeaponDTO> weapons = mapper.readValue(weaponRoot.get("pet_weapon").traverse(), new TypeReference<List<PetWeaponDTO>>() {});
        for (PetWeaponDTO w : weapons) petWeaponMap.put(w.getId(), w);
    }

    public PetConfigDTO.PetDTO getPetById(int id) { return petMap.get(id); }
    public List<PetConfigDTO.PetDTO> getAllPets() { return new ArrayList<>(petMap.values()); }
    public PetItemDTO getPetItemById(int id) { return petItemMap.get(id); }
    public List<PetItemDTO> getAllPetItems() { return new ArrayList<>(petItemMap.values()); }
    public PetWeaponDTO getPetWeaponById(int id) { return petWeaponMap.get(id); }
    public List<PetWeaponDTO> getAllPetWeapons() { return new ArrayList<>(petWeaponMap.values()); }


    // Lấy toàn bộ thông tin pet, ts_gem, cloth của user
    public PetAllInfoDTO getPetAllInfo(String roleId) {
        List<PetEntity> petEntities = petRepository.findByRoleId(roleId);
        List<PetDataDTO> petList = petEntities.stream().map(this::toDTO).toList();

        // Lấy các pet index đang ra trận (tuỳ nghiệp vụ, demo: các petOrder > 0)
        List<Integer> fightPetIndex = petList.stream()
                .filter(p -> p.getPetOrder() != null && p.getPetOrder() > 0)
                .map(PetDataDTO::getPetIndex)
                .toList();

        List<PetTSGemEntity> gemEntities = tsGemRepository.findByRoleId(roleId);
        List<PetTSGemDataDTO> tsGemList = gemEntities.stream().map(this::toDTO).toList();

        List<PetClothEntity> clothEntities = clothRepository.findByRoleId(roleId);
        List<PetClothDataDTO> clothList = clothEntities.stream().map(this::toDTO).toList();

        return PetAllInfoDTO.builder()
                .fightPetIndex(fightPetIndex)
                .petList(petList)
                .tsGemList(tsGemList)
                .clothList(clothList)
                .build();
    }

    // 1. Get 1 pet data
    public PetDataDTO getPetData(String roleId, int petIndex) {
        PetEntity e = petRepository.findByRoleIdAndPetIndex(roleId, petIndex)
                .orElseThrow(() -> new RuntimeException("Pet not found"));
        return toDTO(e);
    }

    // 2. Get 1 ts-gem data
    public PetTSGemDataDTO getTSGemData(String roleId, int gemIndex) {
        PetTSGemEntity e = tsGemRepository.findByRoleIdAndGemIndex(roleId, gemIndex)
                .orElseThrow(() -> new RuntimeException("TSGem not found"));
        return toDTO(e);
    }

    // 3. Get cloth list
    public List<PetClothDataDTO> getClothList(String roleId) {
        return clothRepository.findByRoleId(roleId)
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }
    // 4. Pet operation (level up, advance, ...)
    @Transactional
    public PetOpResultDTO petOperate(String roleId, PetOpRequestDTO req) {
        try {
            switch (req.getReqType()) {
                case 0: // LEVEL_UP (param1: petIndex, param2: exp)
                {
                    PetEntity pet = petRepository.findByRoleIdAndPetIndex(roleId, req.getParam1())
                            .orElseThrow(() -> new NoSuchElementException("Pet not found"));
                    pet.setPetExp(pet.getPetExp() + req.getParam2());
                    // Giả sử mỗi level cần 1000 exp (hoặc dùng bảng config)
                    while (pet.getPetExp() >= 1000) {
                        pet.setPetLevel(pet.getPetLevel() + 1);
                        pet.setPetExp(pet.getPetExp() - 1000);
                    }
                    petRepository.save(pet);
                    return PetOpResultDTO.builder()
                            .retType(0)
                            .retP1(pet.getPetIndex())
                            .retP2(pet.getPetLevel())
                            .build();
                }
                case 1: // ADVANCE (param1: petIndex)
                {
                    PetEntity pet = petRepository.findByRoleIdAndPetIndex(roleId, req.getParam1())
                            .orElseThrow(() -> new NoSuchElementException("Pet not found"));
                    pet.setPetOrder(pet.getPetOrder() + 1);
                    petRepository.save(pet);
                    return PetOpResultDTO.builder()
                            .retType(1)
                            .retP1(pet.getPetIndex())
                            .retP2(pet.getPetOrder())
                            .build();
                }
                case 2: // SET_FIGHT (param1: petIndex)
                {
                    // Đánh dấu petIndex là ra trận (giả lập, có thể update cờ hoặc list riêng)
                    // Ở đây ví dụ tăng capability lên, bạn tùy chỉnh cho logic thực tế
                    PetEntity pet = petRepository.findByRoleIdAndPetIndex(roleId, req.getParam1())
                            .orElseThrow(() -> new NoSuchElementException("Pet not found"));
                    pet.setCapability(pet.getCapability() == null ? 1L : pet.getCapability() + 100L);
                    petRepository.save(pet);
                    return PetOpResultDTO.builder()
                            .retType(2)
                            .retP1(pet.getPetIndex())
                            .retP2(1)
                            .build();
                }
                case 3: // DISCARD (param1: petIndex)
                {
                    petRepository.findByRoleIdAndPetIndex(roleId, req.getParam1())
                            .ifPresent(petRepository::delete);
                    return PetOpResultDTO.builder()
                            .retType(3)
                            .retP1(req.getParam1())
                            .retP2(0)
                            .build();
                }
                case 4: // LOCK_SKILL (param1: petIndex, param2: flag)
                {
                    PetEntity pet = petRepository.findByRoleIdAndPetIndex(roleId, req.getParam1())
                            .orElseThrow(() -> new NoSuchElementException("Pet not found"));
                    pet.setSkillLockFlag(req.getParam2());
                    petRepository.save(pet);
                    return PetOpResultDTO.builder()
                            .retType(4)
                            .retP1(pet.getPetIndex())
                            .retP2(pet.getSkillLockFlag())
                            .build();
                }
                // ...bổ sung thêm case cho các reqType khác
                default:
                    return PetOpResultDTO.builder()
                            .retType(-1).retP1(0).retP2(0)
                            .build();
            }
        } catch (Exception e) {
            return PetOpResultDTO.builder()
                    .retType(-1).retP1(0).retP2(0)
                    .build();
        }
    }

    // 5. One-key up gem

    /**
     * (ví dụ: up toàn bộ các gem truyền vào, tăng level mỗi gemIndex trong list, bạn có thể update logic mạnh hơn nếu muốn)
     * @param roleId
     * @param items
     * @return
     */
    @Transactional
    public PetGemOpResultDTO oneKeyUpLevelGem(String roleId, List<PetOneKeyGemInfoDTO> items) {
        int countSuccess = 0;
        for (PetOneKeyGemInfoDTO item : items) {
            if (Boolean.TRUE.equals(item.getIsTsGem())) {
                tsGemRepository.findByRoleIdAndGemIndex(roleId, item.getTsGemIndex())
                        .ifPresent(gem -> {
                            gem.setGemLevel(gem.getGemLevel() + 1);
                            tsGemRepository.save(gem);
                        });
                countSuccess++;
            }
            // Up thường gem in bag nếu cần
            // Nếu cần update PetEntity.gemItemId hoặc attr thì làm ở đây
        }
        return PetGemOpResultDTO.builder()
                .retType(countSuccess > 0 ? 1 : 0)
                .retP1(countSuccess)
                .retP2(items.size())
                .build();
    }

    // 6. Get evo attr

    /**
     * (giả sử tiến hóa tăng một số thuộc tính nhất định, hoặc truy bảng config, ở đây trả demo +50 cho 3 chỉ số)
     *
     * @param roleId
     * @param petIndex
     * @return
     */
    public PetEvoAttrDTO getPetEvoAttr(String roleId, int petIndex) {
        PetEntity pet = petRepository.findByRoleIdAndPetIndex(roleId, petIndex)
                .orElseThrow(() -> new NoSuchElementException("Pet not found"));
        // Demo: tăng 3 chỉ số, thực tế thì đọc config hoặc tính toán
        List<Integer> evoAttr = Arrays.asList(
                (pet.getAttrList() != null && pet.getAttrList().size() > 0 ? pet.getAttrList().get(0) + 50 : 50),
                (pet.getAttrList() != null && pet.getAttrList().size() > 1 ? pet.getAttrList().get(1) + 50 : 50),
                (pet.getAttrList() != null && pet.getAttrList().size() > 2 ? pet.getAttrList().get(2) + 50 : 50)
        );
        return PetEvoAttrDTO.builder()
                .attrList(evoAttr)
                .build();
    }


    private PetDataDTO toDTO(PetEntity e) {
        return PetDataDTO.builder()
                .petIndex(e.getPetIndex())
                .petId(e.getPetId())
                .petLevel(e.getPetLevel())
                .petExp(e.getPetExp())
                .petOrder(e.getPetOrder())
                .skillList(e.getSkillList())
                .gemItemId(e.getGemItemId())
                .tsGemIndex(e.getTsGemIndex())
                .attrList(e.getAttrList())
                .capability(e.getCapability())
                .skillLockFlag(e.getSkillLockFlag())
                .build();
    }
    private PetTSGemDataDTO toDTO(PetTSGemEntity e) {
        return PetTSGemDataDTO.builder()
                .gemIndex(e.getGemIndex())
                .gemLevel(e.getGemLevel())
                .petIndex(e.getPetIndex())
                .attrType(e.getAttrType())
                .attrValue(e.getAttrValue())
                .build();
    }
    private PetClothDataDTO toDTO(PetClothEntity e) {
        return PetClothDataDTO.builder()
                .itemId(e.getItemId())
                .level(e.getLevel())
                .petIndex(e.getPetIndex())
                .build();
    }



    // --- Logic nâng cấp, advance, buff, ghép đá, random skill ---
    public boolean levelUpPet(int petId, int exp) {
        PetConfigDTO.PetDTO pet = getPetById(petId);
        int curLevel = pet.getSkillGridMax();
        PetConfigDTO.PetUpDTO config = petUpList.stream().filter(up -> up.getPetType() == pet.getPetType() && up.getPetLevel() == curLevel).findFirst().orElse(null);
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
        PetConfigDTO.PetDTO pet = getPetById(petId);
        int order = pet.getPetOrder();
        PetConfigDTO.PetAdvanceDTO advance = petAdvanceList.stream().filter(a -> a.getPetId() == petId && a.getPetOrder() == order + 1).findFirst().orElse(null);
        if (advance == null) return false;
        pet.setPetOrder(order + 1);
        if (advance.getUpAtt() != null) for (PetAttDTO att : advance.getUpAtt()) addPetAttr(pet, att.getType(), att.getAdd());
        if (advance.getUnlockSkillId() != null) pet.getSkillIds().add(advance.getUnlockSkillId());
        return true;
    }

    private void addPetAttr(PetConfigDTO.PetDTO pet, int type, int value) {
        for (PetAttDTO att : pet.getPetAtt()) {
            if (att.getType() == type) { att.setAdd(att.getAdd() + value); return; }
        }
        PetAttDTO newAtt = new PetAttDTO(); newAtt.setType(type); newAtt.setAdd(value); pet.getPetAtt().add(newAtt);
    }

    public void applyBuff(PetConfigDTO.PetDTO pet, int skillId) {
        // Tạo buff giả lập
        PetBuff buff = new PetBuff(); buff.setType(PetBuffType.SPEED); buff.setValue(10); buff.setTurns(5);
        pet.getBuffs().add(buff);
    }

    public int rollRandomSkill(int petId) {
        List<PetConfigDTO.PetSkillDTO> skillPool = petSkillList.stream().filter(s -> s.getSkillColor() == 4).toList();
        Random random = new Random();
        int idx = random.nextInt(skillPool.size());
        return skillPool.get(idx).getSkillId();
    }

    public void useSkill(PetConfigDTO.PetDTO pet, int skillId, PetConfigDTO.PetDTO target) {
        PetConfigDTO.PetSkillDTO skill = petSkillList.stream()
                .filter(s -> s.getSkillId() == skillId)
                .findFirst()
                .orElse(null);
        if (skill == null) return;

        // Ví dụ: Nếu skill này có tăng máu, tăng thủ, gây damage... thì phải mapping ở đây
        // Hiện tại bạn không có thông tin effect, chỉ có thể log skill hoặc update tùy vào skillId

        System.out.println("Pet " + pet.getPetName() + " dùng skill " + skill.getSkillName()
                + " lên " + target.getPetName());

        // Example: Nếu skillId == 1 thì tăng HP cho target
        if (skill.getSkillId() == 1) {
            // Cộng máu vào attrList (giả sử type=1 là HP)
            for (PetAttDTO att : target.getPetAtt()) {
                if (att.getType() == 1) {
                    att.setAdd(att.getAdd() + 6000); // Giá trị tăng lấy từ skill_decs (nếu muốn parse)
                    break;
                }
            }
            // Nếu attr chưa có type=1 thì thêm mới
        }

        // Bạn muốn xử lý gì thêm thì mapping từng skillId thủ công ở đây!
    }

    private void applyEffect(PetConfigDTO.PetDTO target, SkillEffect effect) {
        if ("STUN".equals(effect.getEffectType())) target.setStunned(true);
        if ("DOUBLE_DAMAGE".equals(effect.getEffectType())) target.setNextDamageMultiplier(2.0);
    }

    public String fightBoss(PetConfigDTO.PetDTO playerPet, PetConfigDTO.PetDTO bossPet) {
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
    public String teamBattle(List<PetConfigDTO.PetDTO> teamA, List<PetConfigDTO.PetDTO> teamB) {
        int winA = 0, winB = 0;
        for (int i = 0; i < Math.min(teamA.size(), teamB.size()); i++) {
            String result = fightBoss(teamA.get(i), teamB.get(i));
            if ("WIN".equals(result)) winA++; else winB++;
        }
        return winA > winB ? "TeamA WIN" : (winB > winA ? "TeamB WIN" : "DRAW");
    }
    private int getStat(PetConfigDTO.PetDTO pet, int type) {
        return pet.getPetAtt().stream().filter(a -> a.getType() == type).findFirst().map(PetAttDTO::getAdd).orElse(0);
    }

    public PetStatsDTO calcPetStats(int petId, Integer itemId, Integer weaponId) {
        PetConfigDTO.PetDTO pet = getPetById(petId);
        int hp = getStat(pet, 1), atk = getStat(pet, 2), def = getStat(pet, 3), speed = getStat(pet, 4);
        if (itemId != null) { PetItemDTO item = getPetItemById(itemId); if (item != null) hp += item.getParam(); }
        if (weaponId != null) { PetWeaponDTO weapon = getPetWeaponById(weaponId); if (weapon != null) atk += weapon.getParam(); }
        return new PetStatsDTO(hp, atk, def, speed);
    }
}