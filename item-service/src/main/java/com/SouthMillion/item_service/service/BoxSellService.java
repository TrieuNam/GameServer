package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.BoxRecord;
import com.SouthMillion.item_service.repository.BoxRecordRepository;
import com.SouthMillion.item_service.service.config.RemoteOtherItemConfigServoce;
import org.SouthMillion.dto.item.Box.OtherItemConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BoxSellService {

    @Autowired
    private BoxRecordRepository boxRecordRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RemoteOtherItemConfigServoce otherLoader; // đọc file config cho item "other"

    public BoxRecord sellBoxItem(Long userId, Integer boxId) {
        // 1. Tìm thông tin item từ config-service (hoặc DB)
        OtherItemConfigDTO config = otherLoader.getConfig();
        OtherItemConfigDTO.OtherItem item = config.getOther().stream()
                .filter(o -> Integer.parseInt(o.getId()) == boxId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found: " + boxId));

        // 2. Xử lý bán (cộng/trừ coin, exp...) — bạn có thể viết nghiệp vụ update ở đây
        Integer sellPrice = item.getSellprice() != null && !item.getSellprice().isEmpty()
                ? Integer.parseInt(item.getSellprice()) : 0;
        Integer exp = 0; // tùy nghiệp vụ nếu có trường này

        // 3. Tạo record bán item
        BoxRecord record = new BoxRecord();
        record.setUserId(userId);
        record.setBoxType("other");
        record.setBoxId(boxId);
        record.setRewardItemId(Integer.parseInt(item.getId()));
        record.setRewardNum(1);
        record.setSellPrice(sellPrice);
        record.setExp(exp);
        record.setTimestamp((int) (System.currentTimeMillis() / 1000));

        boxRecordRepository.save(record);

        // 4. Lưu record gần nhất vào Redis (có thể cache nhanh cho response)
        redisTemplate.opsForValue().set("box:last:" + userId, record);

        // 5. Trả lại record (mapping sang DTO nếu expose ra ngoài)
        return record;
    }
}
