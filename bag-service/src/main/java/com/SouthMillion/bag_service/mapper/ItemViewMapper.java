package com.SouthMillion.bag_service.mapper;

import com.SouthMillion.bag_service.enity.BagItem;
import org.SouthMillion.dto.bag.BagDTOs;

public class ItemViewMapper {
    public static BagDTOs.ItemView from(BagItem b) {
        return BagDTOs.ItemView.builder()
                // id là UUID String, không parse sang Long — để null (không dùng ở client)
                .roleId(String.valueOf(b.getRoleId()))
                .itemId(b.getItemId())
                .num(b.getNum() != null ? b.getNum().intValue() : null)
                .bind(b.getBind() ? 1 : 0)
                .expireAt(b.getExpireAt())
                .quality(b.getQuality())
                .bagType(b.getBagType())
                .build();
    }
}
