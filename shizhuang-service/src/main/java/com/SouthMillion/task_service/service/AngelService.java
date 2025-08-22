package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.entity.PlayerAngelEntity;
import com.SouthMillion.task_service.entity.PlayerAngelEquipEntity;
import com.SouthMillion.task_service.entity.PlayerAngelSkinEntity;
import com.SouthMillion.task_service.repository.PlayerAngelEquipRepository;
import com.SouthMillion.task_service.repository.PlayerAngelRepository;
import com.SouthMillion.task_service.repository.PlayerAngelSkinRepository;
import com.SouthMillion.task_service.service.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class AngelService {

    @Autowired
    private  PlayerAngelRepository angelRepository;

    @Autowired
    private  PlayerAngelEquipRepository equipRepository;

    @Autowired
    private  PlayerAngelSkinRepository skinRepository;

    @Autowired
    private  AngelConfigService configService;

    @Autowired
    private  ItemFeignClient itemFeignClient;

    // === Thông tin tổng hợp của thiên sứ ===

    public PlayerAngelEntity getAngelInfo(Long playerId) {
        return angelRepository.findByPlayerId(playerId)
                .orElseGet(() -> {
                    PlayerAngelEntity entity = PlayerAngelEntity.builder()
                            .playerId(playerId)
                            .angelLevel(1)
                            .angelGrade(1)
                            .usedSkinSeq(0)
                            .build();
                    angelRepository.save(entity);
                    return entity;
                });
    }

    /**
     * Trả về info thiên sứ dạng DTO cho 1 player
     */
    public PlayerAngelDTO getAngelInfoDTO(Long playerId) {
        PlayerAngelEntity angel = angelRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new RuntimeException("Player chưa sở hữu thiên sứ"));

        List<PlayerAngelEquipEntity> equips = equipRepository.findByPlayerId(playerId);
        List<PlayerAngelSkinEntity> skins = skinRepository.findByPlayerId(playerId);

        // Map equipId list
        List<Integer> equipIds = equips.stream()
                .map(PlayerAngelEquipEntity::getEquipmentId)
                .toList();

        // Map appearanceDataList
        List<PlayerAngelDTO.AngelAppearanceDTO> appearanceDataList = skins.stream()
                .map(skin -> {
                    PlayerAngelDTO.AngelAppearanceDTO dto = new PlayerAngelDTO.AngelAppearanceDTO();
                    dto.setId(skin.getSkinSeq());
                    dto.setLevel(skin.getSkinLevel());
                    return dto;
                })
                .toList();

        // Build DTO
        PlayerAngelDTO dto = new PlayerAngelDTO();
        dto.setAngelLevel(angel.getAngelLevel());
        dto.setAngelGrade(angel.getAngelGrade());
        dto.setUsedSkinSeq(angel.getUsedSkinSeq());
        dto.setEquipIds(equipIds);
        dto.setAppearanceDataList(appearanceDataList);

        return dto;
    }

    public List<PlayerAngelEquipEntity> getAngelEquips(Long playerId) {
        return equipRepository.findByPlayerId(playerId);
    }

    public List<PlayerAngelSkinEntity> getAngelSkins(Long playerId) {
        return skinRepository.findByPlayerId(playerId);
    }

    /**
     * Tăng bậc thiên sứ cho player
     * @param playerId id người chơi
     */
    public void gradeUpAngel(Long playerId) {
        PlayerAngelEntity angel = angelRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new RuntimeException("Player chưa sở hữu thiên sứ"));

        int nextGrade = angel.getAngelGrade() + 1;

        // Lấy config bậc tiếp theo
        AngelConfigDTO.AngelUpCfg cfg = configService.getAngelStageCfg(nextGrade)
                .orElseThrow(() -> new RuntimeException("Không có config cho bậc thiên sứ: " + nextGrade));

        int needItemId = cfg.getStageItemId();
        int needItemNum = cfg.getStageItemNum();

        // Kiểm tra vật phẩm đủ chưa
        Boolean notEnough = itemFeignClient.isNotEnough(playerId.toString(), needItemId, needItemNum);
        if (Boolean.TRUE.equals(notEnough)) {
            throw new RuntimeException("Không đủ vật phẩm tăng bậc thiên sứ!");
        }

        // Trừ vật phẩm
        boolean consumed = itemFeignClient.consume(playerId.toString(), needItemId, needItemNum);
        if (!consumed) {
            throw new RuntimeException("Lỗi khi trừ vật phẩm tăng bậc!");
        }

        // Tăng bậc và lưu lại
        angel.setAngelGrade(nextGrade);
        angelRepository.save(angel);
    }



    // === Nâng cấp thiên sứ (Level Up) ===

    public boolean levelUpAngel(Long playerId) {
        PlayerAngelEntity angel = getAngelInfo(playerId);
        int nextLevel = angel.getAngelLevel() + 1;
        AngelConfigDTO.AngelLevelCfg cfg = configService.getAngelLevelCfg(nextLevel)
                .orElseThrow(() -> new RuntimeException("Không có cấu hình level: " + nextLevel));

        // Kiểm tra vật phẩm đủ không
        if (itemFeignClient.isNotEnough(playerId.toString(), cfg.getUpItemId(), cfg.getUpItemNum())) {
            throw new RuntimeException("Không đủ vật phẩm nâng cấp thiên sứ!");
        }
        // Trừ vật phẩm
        boolean consumed = itemFeignClient.consume(playerId.toString(), cfg.getUpItemId(), cfg.getUpItemNum());
        if (!consumed) throw new RuntimeException("Lỗi khi trừ vật phẩm!");

        angel.setAngelLevel(nextLevel);
        angelRepository.save(angel);
        return true;
    }



    /**
     * Lấy config bậc thiên sứ hiện tại của player
     */
    public AngelConfigDTO.AngelUpCfg getCurrentAngelStageCfg(Long playerId) {
        // Lấy entity thiên sứ của player
        PlayerAngelEntity angel = angelRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new RuntimeException("Player chưa sở hữu thiên sứ!"));

        int currentGrade = angel.getAngelGrade();

        // Lấy config bậc tương ứng từ angelConfigService
        return configService.getAngelStageCfg(currentGrade)
                .orElseThrow(() -> new RuntimeException("Không có cấu hình bậc: " + currentGrade));
    }


    // === Nâng cấp trang bị thiên sứ ===

    public boolean levelUpEquip(Long playerId, int position) {
        PlayerAngelEquipEntity equip = equipRepository.findByPlayerIdAndPosition(playerId, position)
                .orElseThrow(() -> new RuntimeException("Chưa có trang bị ở vị trí: " + position));

        AngelConfigDTO.AngelEquipUpCfg cfg = configService.getAngelConfig().getEquipmentUp().stream()
                .filter(c -> c.getPosition() == position && c.getEquipmentId() == equip.getEquipmentId())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config trang bị: pos=" + position));

        // Kiểm tra từng loại vật phẩm
        if (itemFeignClient.isNotEnough(playerId.toString(), cfg.getUpItemId0(), cfg.getUpItemNum0())
                || itemFeignClient.isNotEnough(playerId.toString(), cfg.getUpItemId1(), cfg.getUpItemNum1())) {
            throw new RuntimeException("Không đủ vật phẩm nâng cấp trang bị!");
        }
        // Trừ vật phẩm
        boolean consumed0 = itemFeignClient.consume(playerId.toString(), cfg.getUpItemId0(), cfg.getUpItemNum0());
        boolean consumed1 = itemFeignClient.consume(playerId.toString(), cfg.getUpItemId1(), cfg.getUpItemNum1());
        if (!consumed0 || !consumed1) throw new RuntimeException("Lỗi khi trừ vật phẩm!");

        equip.setLevel(equip.getLevel() + 1);
        equipRepository.save(equip);
        return true;
    }

    // === Kích hoạt skin & dùng skin ===

    public boolean activateSkin(Long playerId, int skinSeq) {
        Optional<PlayerAngelSkinEntity> optSkin = skinRepository.findByPlayerIdAndSkinSeq(playerId, skinSeq);
        if (optSkin.isPresent() && optSkin.get().isActivated())
            throw new RuntimeException("Skin đã kích hoạt!");

        AngelConfigDTO.AngelSkinCfg skinCfg = configService.getAngelConfig().getAngelRes().stream()
                .filter(cfg -> cfg.getAngleSkinSeq() == skinSeq)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config skin: " + skinSeq));

        // Kiểm tra vật phẩm kích hoạt skin
        if (itemFeignClient.isNotEnough(playerId.toString(), skinCfg.getJihuoItemId(), 1)) {
            throw new RuntimeException("Không đủ vật phẩm kích hoạt skin!");
        }
        boolean consumed = itemFeignClient.consume(playerId.toString(), skinCfg.getJihuoItemId(), 1);
        if (!consumed) throw new RuntimeException("Lỗi khi trừ vật phẩm!");

        PlayerAngelSkinEntity skin = optSkin.orElse(PlayerAngelSkinEntity.builder()
                .playerId(playerId)
                .skinSeq(skinSeq)
                .skinLevel(1)
                .activated(true)
                .build());
        skin.setActivated(true);
        skinRepository.save(skin);
        return true;
    }

    public boolean useSkin(Long playerId, int skinSeq) {
        PlayerAngelEntity angel = getAngelInfo(playerId);
        PlayerAngelSkinEntity skin = skinRepository.findByPlayerIdAndSkinSeq(playerId, skinSeq)
                .orElseThrow(() -> new RuntimeException("Skin chưa kích hoạt"));
        if (!skin.isActivated()) throw new RuntimeException("Skin chưa kích hoạt");

        angel.setUsedSkinSeq(skinSeq);
        angelRepository.save(angel);
        return true;
    }

    // === Nâng cấp skin ===

    public boolean levelUpSkin(Long playerId, int skinSeq) {
        PlayerAngelSkinEntity skin = skinRepository.findByPlayerIdAndSkinSeq(playerId, skinSeq)
                .orElseThrow(() -> new RuntimeException("Skin chưa kích hoạt"));

        int nextLevel = skin.getSkinLevel() + 1;
        AngelConfigDTO.AngelSkinUpCfg skinUpCfg = configService.getAngelConfig().getAngelResUp().stream()
                .filter(cfg -> cfg.getAngleSkinSeq() == skinSeq && cfg.getSkinLevel() == nextLevel)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config nâng cấp skin: " + skinSeq + ", level " + nextLevel));

        // Kiểm tra vật phẩm nâng cấp skin
        if (itemFeignClient.isNotEnough(playerId.toString(), skinUpCfg.getUpItemId(), skinUpCfg.getUpItemNum())) {
            throw new RuntimeException("Không đủ vật phẩm nâng cấp skin!");
        }
        boolean consumed = itemFeignClient.consume(playerId.toString(), skinUpCfg.getUpItemId(), skinUpCfg.getUpItemNum());
        if (!consumed) throw new RuntimeException("Lỗi khi trừ vật phẩm!");

        skin.setSkinLevel(nextLevel);
        skinRepository.save(skin);
        return true;
    }

    // === Các hàm tiện ích: Lấy config từng phần ===

    public AngelConfigDTO.AngelLevelCfg getLevelConfig(int level) {
        return configService.getAngelLevelCfg(level)
                .orElseThrow(() -> new RuntimeException("Không có config level: " + level));
    }

    public AngelConfigDTO.AngelUpCfg getGradeConfig(int grade) {
        return configService.getAngelStageCfg(grade)
                .orElseThrow(() -> new RuntimeException("Không có config grade: " + grade));
    }

    public AngelConfigDTO.AngelEquipUpCfg getEquipConfig(int position, int equipId) {
        return configService.getAngelConfig().getEquipmentUp().stream()
                .filter(cfg -> cfg.getPosition() == position && cfg.getEquipmentId() == equipId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config trang bị: pos=" + position + ", id=" + equipId));
    }

    public AngelConfigDTO.AngelSkinCfg getSkinConfig(int skinSeq) {
        return configService.getAngelConfig().getAngelRes().stream()
                .filter(cfg -> cfg.getAngleSkinSeq() == skinSeq)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config skin: " + skinSeq));
    }

    public AngelConfigDTO.AngelSkinUpCfg getSkinUpConfig(int skinSeq, int skinLevel) {
        return configService.getAngelConfig().getAngelResUp().stream()
                .filter(cfg -> cfg.getAngleSkinSeq() == skinSeq && cfg.getSkinLevel() == skinLevel)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có config nâng cấp skin: " + skinSeq + ", level " + skinLevel));
    }
}