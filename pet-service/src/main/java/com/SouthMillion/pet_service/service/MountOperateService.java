package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.entity.PlayerHarnessEntity;
import com.SouthMillion.pet_service.entity.PlayerMountEntity;
import com.SouthMillion.pet_service.repository.PlayerHarnessRepository;
import com.SouthMillion.pet_service.repository.PlayerMountRepository;
import com.SouthMillion.pet_service.service.client.BagFeignClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.Knapsack.HarnessItemConfigDTO;
import org.SouthMillion.exception.BizException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MountOperateService {
    private final PlayerMountRepository playerMountRepository;
    private final PlayerHarnessRepository playerHarnessRepository;
    private final MountConfigService mountConfigService;
    private final BagFeignClient bagFeignClient; // FeignClient sang item-service/bag-service

    // --- 1. Nâng cấp mount (LEVEL_UP) ---
    @Transactional
    public void levelUpMount(String playerId, Integer mountId) {
        PlayerMountEntity mount = playerMountRepository.findByPlayerIdAndMountId(playerId, mountId)
                .orElseThrow(() -> new BizException("Bạn chưa sở hữu cưỡi này!"));

        ModelItemConfigDTO modelCfg = mountConfigService.getModel(mountId);
        if (modelCfg == null) throw new BizException("Không tìm thấy config mount!");

        int currentLevel = mount.getLevel();
        ModelItemConfigDTO.LevelUpConfig nextCfg = modelCfg.getModelAfter().stream()
                .filter(cfg -> cfg.getLevel() == currentLevel + 1)
                .findFirst().orElse(null);

        if (nextCfg == null) throw new BizException("Cưỡi đã đạt cấp tối đa!");

        // Kiểm tra điều kiện mở level
        if (nextCfg.getLimitGrade() != null && mount.getGrade() < nextCfg.getLimitGrade())
            throw new BizException("Chưa đủ bậc để nâng cấp!");

        // Kiểm tra yêu cầu level nhân vật/vip nếu có (bổ sung khi kết nối user-service)

        // Kiểm tra tài nguyên: vàng
        if (nextCfg.getCostGold() != null && nextCfg.getCostGold() > 0) {
            if (!bagFeignClient.hasEnoughGold(playerId, nextCfg.getCostGold()))
                throw new BizException("Không đủ vàng!");
        }

        // Kiểm tra vật phẩm tiêu hao (đa vật phẩm)
        if (nextCfg.getCostItems() != null) {
            for (ModelItemConfigDTO.LevelUpConfig.CostItem ci : nextCfg.getCostItems()) {
                if (!bagFeignClient.hasEnough(playerId, ci.getItemId(), ci.getItemNum()))
                    throw new BizException("Không đủ vật phẩm ID: " + ci.getItemId());
            }
        }

        // Trừ tài nguyên: vàng, item
        if (nextCfg.getCostGold() != null && nextCfg.getCostGold() > 0)
            bagFeignClient.consumeGold(playerId, nextCfg.getCostGold());

        if (nextCfg.getCostItems() != null) {
            for (ModelItemConfigDTO.LevelUpConfig.CostItem ci : nextCfg.getCostItems()) {
                bagFeignClient.consumeItem(playerId, ci.getItemId(), ci.getItemNum());
            }
        }

        // Cộng level
        mount.setLevel(currentLevel + 1);
        playerMountRepository.save(mount);

        // (Optional) Trả về thông tin mới
        // return mapToProto(mount);
    }

    // --- 2. Nâng bậc mount (GRADE_UP) ---
    @Transactional
    public void gradeUpMount(String playerId, Integer mountId) {
        PlayerMountEntity mount = playerMountRepository.findByPlayerIdAndMountId(playerId, mountId)
                .orElseThrow(() -> new BizException("Bạn chưa sở hữu cưỡi này!"));

        // Giả sử cấu hình nâng bậc nằm trong modelCfg.getGradeUpList()
        ModelItemConfigDTO modelCfg = mountConfigService.getModel(mountId);
        if (modelCfg == null) throw new BizException("Không tìm thấy config mount!");

        int currentGrade = mount.getGrade();
        GradeUpConfig nextCfg = modelCfg.getGradeUpList().stream()
                .filter(cfg -> cfg.getGrade() == currentGrade + 1)
                .findFirst().orElse(null);

        if (nextCfg == null) throw new BizException("Cưỡi đã đạt bậc tối đa!");

        // Kiểm tra nguyên liệu & vàng
        if (nextCfg.getCostGold() != null && nextCfg.getCostGold() > 0) {
            if (!bagFeignClient.hasEnoughGold(playerId, nextCfg.getCostGold()))
                throw new BizException("Không đủ vàng!");
        }
        if (nextCfg.getCostItems() != null) {
            for (GradeUpConfig.CostItem ci : nextCfg.getCostItems()) {
                if (!bagFeignClient.hasEnough(playerId, ci.getItemId(), ci.getItemNum()))
                    throw new BizException("Không đủ vật phẩm ID: " + ci.getItemId());
            }
        }

        // Trừ tài nguyên
        if (nextCfg.getCostGold() != null && nextCfg.getCostGold() > 0)
            bagFeignClient.consumeGold(playerId, nextCfg.getCostGold());
        if (nextCfg.getCostItems() != null) {
            for (GradeUpConfig.CostItem ci : nextCfg.getCostItems()) {
                bagFeignClient.consumeItem(playerId, ci.getItemId(), ci.getItemNum());
            }
        }

        // Cộng bậc
        mount.setGrade(currentGrade + 1);
        playerMountRepository.save(mount);
    }

    // --- 3. Trang bị harness (WEAR) ---
    @Transactional
    public void wearHarness(String playerId, Integer harnessId) {
        PlayerHarnessEntity harness = playerHarnessRepository.findByPlayerIdAndHarnessId(playerId, harnessId)
                .orElseThrow(() -> new BizException("Không sở hữu harness này!"));
        if (harness.getWearingMark() == 1) throw new BizException("Harness đã được trang bị!");

        harness.setWearingMark(1); // Đánh dấu đã trang bị
        playerHarnessRepository.save(harness);
        // (Optional) update state mount player, validate slot, v.v.
    }

    // --- 4. Phân giải harness (DECOMPOSE) ---
    @Transactional
    public void decomposeHarness(String playerId, Integer harnessId) {
        PlayerHarnessEntity harness = playerHarnessRepository.findByPlayerIdAndHarnessId(playerId, harnessId)
                .orElseThrow(() -> new BizException("Không sở hữu harness này!"));
        if (harness.getWearingMark() == 1)
            throw new BizException("Không thể phân giải harness đang trang bị!");

        // Lấy thông tin phần thưởng phân giải từ config
        HarnessItemConfigDTO harnessCfg = mountConfigService.getHarness(harnessId);
        if (harnessCfg == null) throw new BizException("Không tìm thấy config harness!");

        Integer rewardItemId = harnessCfg.getSellItemId();
        Integer rewardItemNum = harnessCfg.getSellItemNum();
        if (rewardItemId == null || rewardItemNum == null || rewardItemNum <= 0)
            throw new BizException("Config phần thưởng phân giải thiếu!");

        // Xóa harness khỏi túi
        playerHarnessRepository.delete(harness);

        // Thưởng vật phẩm cho player
        bagFeignClient.addItem(playerId, rewardItemId, rewardItemNum);
    }

    // --- 5. Tẩy luyện thuộc tính harness (ENTRY_REFRESH) ---
    @Transactional
    public void refreshEntry(String playerId, Integer harnessId, Integer lockFlag, Integer consumeItemId) {
        PlayerHarnessEntity harness = playerHarnessRepository.findByPlayerIdAndHarnessId(playerId, harnessId)
                .orElseThrow(() -> new BizException("Không sở hữu harness này!"));

        // Kiểm tra tiêu hao
        if (!bagFeignClient.hasEnough(playerId, consumeItemId, 1))
            throw new BizException("Không đủ vật phẩm tẩy luyện!");

        bagFeignClient.consumeItem(playerId, consumeItemId, 1);

        // Lấy cấu hình harness, tỉ lệ thuộc tính mới (bổ sung nếu có config random attr)
        HarnessItemConfigDTO harnessCfg = mountConfigService.getHarness(harnessId);

        // TODO: random lại thuộc tính theo config, skip các attr bị khóa bởi lockFlag
        // harness.setAttrType(...); harness.setAttrValue(...);

        playerHarnessRepository.save(harness);
    }
}