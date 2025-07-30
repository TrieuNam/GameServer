package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.BoxRecord;
import com.SouthMillion.item_service.repository.BoxRecordRepository;
import com.SouthMillion.item_service.service.config.*;
import org.SouthMillion.dto.item.Box.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class BoxOpenService {
    @Autowired
    private RemoteUnpackConfigService unpackLoader;
    @Autowired private RemoteKXDJConfigService kxdjLoader;
    @Autowired private RemoteBXJJConfigService bxjjLoader;
    @Autowired private RemoteBXZYConfigService bxzyLoader;
    @Autowired private RemoteGiftConfigService giftLoader;
    @Autowired private RemoteOtherItemConfigServoce otherLoader;
    @Autowired private BoxRecordRepository boxRecordRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    public BoxRecord openBox(BoxOpenRequestDTO req) {
        Long userId = req.getUserId();
        String boxType = req.getBoxType();
        Integer boxId = req.getBoxId();
        Map<String, Object> params = req.getExtraParams();

        BoxRecord record = null;
        switch (boxType) {
            case "unpack": {
                UnpackConfigDTO config = unpackLoader.getConfig();
                // 2. Tìm RandomColor đúng boxId
                UnpackConfigDTO.RandomColor colorConfig = config.getRandomColor().stream()
                        .filter(c -> Integer.parseInt(c.getBoxLevel()) == boxId)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Box level not found: " + boxId));
                // 3. Random phần thưởng (do không có rate -> random đều)
                UnpackConfigDTO.RewardItem reward = randomReward(colorConfig.getReward());
                // 4. Build record
                 record = buildRecord(userId, boxType, boxId, reward, params);
                break;
            }
            case "gift": {
                GiftConfigDTO config = giftLoader.getConfig();
                GiftConfigDTO.Gift gift = config.getDefGift().stream()
                        .filter(g -> Integer.parseInt(g.getId()) == boxId)
                        .findFirst().orElseThrow(() -> new RuntimeException("Gift not found"));
                GiftConfigDTO.GiftItem reward = randomGift(gift.getGift());
                record = buildRecord(req.getUserId(), boxType, boxId, reward, params);
                break;
            }
            case "baoxiangjijin": {
                BXJJConfigDTO config = bxjjLoader.getConfig();
                BXJJConfigDTO.GiftConfigure giftConf = config.getGiftConfigure().stream()
                        .filter(g -> Integer.parseInt(g.getPhase()) == (int)params.getOrDefault("phase", 1))
                        .findFirst().orElseThrow(() -> new RuntimeException("BXJJ config not found"));

                BXJJConfigDTO.OrdinaryItem reward;
                // Nếu muốn lấy random 1 item từ seniorItem (nếu truyền param, ví dụ: params.get("senior") == true)
                if (params != null && params.getOrDefault("senior", false).equals(Boolean.TRUE)
                        && giftConf.getSeniorItem() != null && !giftConf.getSeniorItem().isEmpty()) {
                    List<BXJJConfigDTO.OrdinaryItem> seniorList = giftConf.getSeniorItem();
                    int idx = new Random().nextInt(seniorList.size());
                    reward = seniorList.get(idx);
                } else {
                    reward = giftConf.getOrdinaryItem();
                }
                record = buildRecord(userId, boxType, boxId, reward, params);
                break;
            }
            case "baoxiangzhuangyuan": {
                BXZYConfigDTO config = bxzyLoader.getConfig();
                BXZYConfigDTO.Reward rewardRow = config.getReward().stream()
                        .filter(r -> Integer.parseInt(r.getSeq()) == boxId)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("BXZY reward not found"));
                List<BXZYConfigDTO.RewardItem> rewardItems = rewardRow.getRewardItem();
                BXZYConfigDTO.RewardItem reward;
                if (rewardItems != null && !rewardItems.isEmpty()) {
                    int idx = new Random().nextInt(rewardItems.size());
                    reward = rewardItems.get(idx);
                } else {
                    throw new RuntimeException("No reward item for BXZY with seq=" + boxId);
                }
                record = buildRecord(userId, boxType, boxId, reward, params);
                break;
            }
            case "kaixiangdaji": {
                KXDJConfigDTO config = kxdjLoader.getConfig();
                KXDJConfigDTO.Reward rewardRow = config.getReward().stream()
                        .filter(r -> Integer.parseInt(r.getType()) == (int) params.getOrDefault("type", 0))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("KXDJ reward not found"));
                KXDJConfigDTO.RewardItem reward = rewardRow.getRewardItem();
                record = buildRecord(userId, boxType, boxId, reward, params);
            }
            case "other": {
                OtherItemConfigDTO config = otherLoader.getConfig();
                OtherItemConfigDTO.OtherItem item = config.getOther().stream()
                        .filter(o -> Integer.parseInt(o.getId()) == boxId)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Other item not found"));
                record = buildRecord(userId, boxType, boxId, item, params);
                break;
            }
            default:
                throw new RuntimeException("Not support boxType=" + boxType);
        }

        boxRecordRepository.save(record);
        redisTemplate.opsForValue().set("box:last:" + userId, record);
        return record;
    }




    // ---- RANDOM & BUILD RECORD ----
    private UnpackConfigDTO.RewardItem randomReward(List<UnpackConfigDTO.RewardItem> rewards) {
        if (rewards == null || rewards.isEmpty())
            throw new RuntimeException("No rewards configured!");
        int idx = new Random().nextInt(rewards.size());
        return rewards.get(idx);
    }

    private GiftConfigDTO.GiftItem randomGift(List<GiftConfigDTO.GiftItem> gifts) {
        int total = gifts.stream().mapToInt(r -> Integer.parseInt(r.getRate())).sum();
        int rand = new Random().nextInt(total);
        int curr = 0;
        for (GiftConfigDTO.GiftItem g : gifts) {
            curr += Integer.parseInt(g.getRate());
            if (rand < curr) return g;
        }
        return gifts.get(0);
    }

    // Xử lý mapping toàn bộ field từ json/params
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, UnpackConfigDTO.RewardItem reward, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(reward.getItemId() != null ? Integer.parseInt(reward.getItemId()) : null);
        record.setRewardNum(reward.getNum() != null ? Integer.parseInt(reward.getNum()) : 1);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("sellPrice")) record.setSellPrice((Integer) params.get("sellPrice"));
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, GiftConfigDTO.GiftItem reward, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(reward.getItemId() != null ? Integer.parseInt(reward.getItemId()) : null);
        record.setRewardNum(reward.getNum() != null ? Integer.parseInt(reward.getNum()) : 1);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("sellPrice")) record.setSellPrice((Integer) params.get("sellPrice"));
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
    // Các buildRecord cho BXJJConfigDTO.OrdinaryItem, BXZYConfigDTO.RewardItem, KXDJConfigDTO.RewardItem, OtherItemConfigDTO.OtherItem tương tự như trên.
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, BXJJConfigDTO.OrdinaryItem reward, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(reward.getItemId() != null ? Integer.parseInt(reward.getItemId()) : null);
        record.setRewardNum(reward.getNum() != null ? Integer.parseInt(reward.getNum()) : 1);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("sellPrice")) record.setSellPrice((Integer) params.get("sellPrice"));
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, BXZYConfigDTO.RewardItem reward, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(reward.getItemId() != null ? Integer.parseInt(reward.getItemId()) : null);
        record.setRewardNum(reward.getNum() != null ? Integer.parseInt(reward.getNum()) : 1);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("sellPrice")) record.setSellPrice((Integer) params.get("sellPrice"));
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, KXDJConfigDTO.RewardItem reward, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(reward.getItemId() != null ? Integer.parseInt(reward.getItemId()) : null);
        record.setRewardNum(reward.getNum() != null ? Integer.parseInt(reward.getNum()) : 1);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("sellPrice")) record.setSellPrice((Integer) params.get("sellPrice"));
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
    private BoxRecord buildRecord(Long userId, String boxType, Integer boxId, OtherItemConfigDTO.OtherItem item, Map<String, Object> params) {
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType(boxType);
        record.setBoxId(boxId);
        record.setRewardItemId(item.getId() != null ? Integer.parseInt(item.getId()) : null);
        record.setRewardNum(1); // vì other chỉ là 1 item đặc biệt
        if (item.getSellprice() != null && !item.getSellprice().isEmpty()) {
            record.setSellPrice(Integer.parseInt(item.getSellprice()));
        }
        // Có thể set các field khác nếu bạn cần (itemType, isVirtual...) vào record (nếu có)
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));
        if (params != null) {
            if (params.containsKey("exp")) record.setExp((Integer) params.get("exp"));
        }
        return record;
    }
}
