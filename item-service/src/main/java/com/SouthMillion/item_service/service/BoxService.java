package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.BoxRecord;
import com.SouthMillion.item_service.mapper.BoxRecordMapper;
import com.SouthMillion.item_service.repository.BoxRecordRepository;
import org.SouthMillion.dto.item.Box.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class BoxService {

    @Autowired
    private BoxOpenService boxOpenService;
    @Autowired
    private BoxAutoOpenService boxAutoOpenService;
    @Autowired
    private BoxSellService boxSellService;
    @Autowired
    private BoxRecordRepository boxRecordRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Mở hộp
    public BoxRecordDTO openBox(Long userId, BoxOperationDTO dto) {
        BoxOpenRequestDTO req = mapBoxOperationDTO(userId, dto);
        BoxRecord entity = boxOpenService.openBox(req);
        return BoxRecordMapper.toDTO(entity);
    }

    // Lưu setting auto box
    public void saveBoxSetting(Long userId, BoxSetDTO setting) {
        boxAutoOpenService.saveBoxSetting(userId, setting);
    }

    // Lấy setting auto box
    public BoxSetDTO getBoxSetting(Long userId) {
        return boxAutoOpenService.getBoxSetting(userId);
    }

    // Bán box hoặc trang bị
    public BoxRecordDTO sellBoxItem(Long userId, Integer boxId) {
        BoxRecord entity = boxSellService.sellBoxItem(userId, boxId);
        return BoxRecordMapper.toDTO(entity);
    }

    // Lấy record mở hộp gần nhất
    public BoxRecordDTO getLastBoxInfo(Long userId) {
        BoxRecord entity = (BoxRecord) redisTemplate.opsForValue().get("box:last:" + userId);
        if (entity == null) {
            entity = boxRecordRepository.findTopByUserIdOrderByTimestampDesc(userId);
        }
        return BoxRecordMapper.toDTO(entity);
    }

    // Hàm mapping giữa BoxOperationDTO -> BoxOpenRequestDTO
    private BoxOpenRequestDTO mapBoxOperationDTO(Long userId, BoxOperationDTO dto) {
        BoxOpenRequestDTO req = new BoxOpenRequestDTO();
        req.setUserId(userId);

        // Mapping logic tùy reqType (nếu cần)
        switch (dto.getReqType()) {
            case 1:
                req.setBoxType("unpack");
                break;
            case 2:
                req.setBoxType("equip");
                break;
            case 3:
                req.setBoxType("sell");
                break;
            default:
                req.setBoxType("other");
        }
        req.setBoxId(dto.getParam());
        req.setExtraParams(new HashMap<>()); // nếu mở rộng BoxOperationDTO, truyền extra param tại đây
        return req;
    }

    // Xử lý các loại request box
    public BoxInfoDTO handleBoxRequest(Long userId, int reqType, int param) {
        // Xử lý từng loại reqType: mở hộp, trang bị, bán, nâng cấp, v.v...
        // Đây là ví dụ trả về dummy, bạn thay bằng nghiệp vụ thật sự!
        BoxInfoDTO dto = new BoxInfoDTO();
        dto.setBoxLevel(5);
        dto.setBuyTimes(3);
        dto.setTimestamp(System.currentTimeMillis() / 1000);
        dto.setArenaItemNum(2);
        dto.setShiZhuangNum(1);
        dto.setLevelFetchFlag(1);

        // TODO: Xử lý thật theo userId, reqType, param
        return dto;
    }
}