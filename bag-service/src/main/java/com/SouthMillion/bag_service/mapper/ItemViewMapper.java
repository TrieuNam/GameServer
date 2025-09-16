package com.SouthMillion.bag_service.mapper;

import com.SouthMillion.bag_service.enity.BagItem;
import org.SouthMillion.dto.bag.BagDTOs;

public class ItemViewMapper {
    public static BagDTOs.ItemView from(BagItem b) {
        return BagDTOs.ItemView.builder()
                .id(b.getId())
                .roleId(b.getRoleId())
                .itemId(b.getItemId())
                .num(b.getNum())
                .bind(b.getBind())
                .expireAt(b.getExpireAt())
                .build();
    }
}
