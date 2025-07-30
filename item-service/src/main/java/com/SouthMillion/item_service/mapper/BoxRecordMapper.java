package com.SouthMillion.item_service.mapper;

import com.SouthMillion.item_service.entity.BoxRecord;
import org.SouthMillion.dto.item.Box.BoxRecordDTO;

public class BoxRecordMapper {
    public static BoxRecordDTO toDTO(BoxRecord entity) {
        if (entity == null) return null;
        BoxRecordDTO dto = new BoxRecordDTO();
        dto.setUserId(entity.getUserId());
        dto.setBoxType(entity.getBoxType());
        dto.setBoxId(entity.getBoxId());
        dto.setRewardItemId(entity.getRewardItemId());
        dto.setRewardNum(entity.getRewardNum());
        dto.setSellPrice(entity.getSellPrice());
        dto.setExp(entity.getExp());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}