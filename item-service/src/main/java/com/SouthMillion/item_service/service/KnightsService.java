package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.KnightsProgressEntity;
import com.SouthMillion.item_service.repository.KnightsProgressRepository;
import com.SouthMillion.item_service.service.client.TaskFeignClient;
import com.SouthMillion.item_service.service.client.UserResourceFeignClient;
import com.SouthMillion.item_service.service.config.JsonKnightConfigService;
import jakarta.transaction.Transactional;
import org.SouthMillion.dto.item.knights.KnightsBookDTO;
import org.SouthMillion.dto.item.knights.KnightsConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KnightsService {
    @Autowired
    private ItemService itemService;

    @Autowired
    private KnightsProgressRepository knightsRepo;

    @Autowired
    private JsonKnightConfigService configService;

    @Autowired
    private UserResourceFeignClient userService;

    @Autowired
    private TaskFeignClient taskFeignClient;


    public KnightsProgressEntity getOrCreateUserProgress(String userId) {
        return knightsRepo.findByUserId(userId).orElseGet(() -> {
            KnightsProgressEntity entity = new KnightsProgressEntity();
            entity.setUserId(userId);
            entity.setLevel(1);
            entity.setFlag(0);
            entity.setLevelFlag(0);
            return knightsRepo.save(entity);
        });
    }

    @Transactional
    public void handleOperation(String userId, int opType, int param) {
        KnightsProgressEntity entity = getOrCreateUserProgress(userId);
        KnightsConfigDTO config = configService.getKnightsConfig();

        switch (opType) {
            case 1: // Nâng cấp level
                int nextLevel = entity.getLevel() + 1;
                Optional<KnightsBookDTO> bookOpt = config.getKnightsBook().stream()
                        .filter(e -> e.getLevel() == nextLevel)
                        .findFirst();
                if (bookOpt.isPresent()) {
                    KnightsBookDTO book = bookOpt.get();
                    if (checkUpgradeCondition(userId, book)) {
                        entity.setLevel(nextLevel);
                        knightsRepo.save(entity);
                    } else {
                        throw new RuntimeException("Không đủ điều kiện nâng cấp Knights!");
                    }
                } else {
                    throw new RuntimeException("Đã đạt cấp tối đa Knights!");
                }
                break;

            case 2: // Nhận thưởng theo seq (dùng param là seq)
                int seq = param;
                if (isSeqRewardAvailable(entity, config, seq)) {
                    entity.setFlag(entity.getFlag() | (1 << (seq - 1)));
                    knightsRepo.save(entity);
                    // Gọi service phát thưởng cho user nếu cần
                } else {
                    throw new RuntimeException("Không thể nhận thưởng seq này!");
                }
                break;

            case 3: // Nhận thưởng theo level (dùng param là level)
                int claimLevel = param;
                if (isLevelRewardAvailable(entity, claimLevel)) {
                    entity.setLevelFlag(entity.getLevelFlag() | (1 << (claimLevel - 1)));
                    knightsRepo.save(entity);
                    // Gọi service phát thưởng cho user nếu cần
                } else {
                    throw new RuntimeException("Không thể nhận thưởng level này!");
                }
                break;
        }
    }

    public boolean checkUpgradeCondition(String userId, KnightsBookDTO book) {
        int condition = book.getCondition();
        int param1 = book.getParam1();

        switch (condition) {
            case 0: // Không có điều kiện
                return true;
            case 1: // Yêu cầu level user >= param1
                int userLevel = userService.getUserLevel(userId);
                return userLevel >= param1;
            case 2: // Yêu cầu có vật phẩm
                int itemCount = itemService.getUserItemCount(userId, param1);
                return itemCount >= 1;
            case 3: // Yêu cầu đã hoàn thành task
                boolean taskDone = taskFeignClient.hasCompletedTask(userId, param1);
                return taskDone;
            default:
                return false;
        }
    }

    public boolean isSeqRewardAvailable(KnightsProgressEntity entity, KnightsConfigDTO config, int seq) {
        boolean notClaimed = ((entity.getFlag() >> (seq - 1)) & 1) == 0;
        Optional<KnightsBookDTO> bookOpt = config.getKnightsBook().stream()
                .filter(e -> e.getSeq() == seq)
                .findFirst();
        if (bookOpt.isEmpty()) return false;
        KnightsBookDTO book = bookOpt.get();
        boolean hasLevel = entity.getLevel() >= book.getLevel();
        return notClaimed && hasLevel;
    }

    public boolean isLevelRewardAvailable(KnightsProgressEntity entity, int level) {
        boolean notClaimed = ((entity.getLevelFlag() >> (level - 1)) & 1) == 0;
        boolean hasLevel = entity.getLevel() >= level;
        return notClaimed && hasLevel;
    }

    public KnightsProgressEntity getKnightsInfo(String userId) {
        return getOrCreateUserProgress(userId);
    }

    public List<Integer> getCurrentConditionList(String userId) {
        KnightsConfigDTO config = configService.getKnightsConfig();
        KnightsProgressEntity entity = getKnightsInfo(userId);
        List<KnightsBookDTO> books = config.getKnightsBook()
                .stream().filter(b -> b.getSeq() <= 3)
                .collect(Collectors.toList());
        List<Integer> conditionStates = new ArrayList<>();
        for (KnightsBookDTO book : books) {
            boolean ok = checkUpgradeCondition(userId, book);
            conditionStates.add(ok ? 1 : 0);
        }
        while (conditionStates.size() < 3) conditionStates.add(0);
        return conditionStates;
    }
}
