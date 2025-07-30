package com.SouthMillion.item_service.mapper;

import com.SouthMillion.item_service.entity.MysteryShopStateEntity;
import com.SouthMillion.item_service.entity.ShopPurchaseEntity;
import com.SouthMillion.item_service.entity.UserCoreLimitEntity;
import org.SouthMillion.proto.Msgother.Msgother;

import java.util.List;

public class ShopMapper {
    public static Msgother.PB_ShopData toProto(ShopPurchaseEntity e) {
        return Msgother.PB_ShopData.newBuilder()
                .setIndex(e.getShopIndex())
                .setBuyNum(e.getBuyNum())
                .build();
    }

    public static Msgother.PB_SCShopInfo toShopInfoProto(List<ShopPurchaseEntity> list) {
        Msgother.PB_SCShopInfo.Builder builder = Msgother.PB_SCShopInfo.newBuilder();
        for (ShopPurchaseEntity e : list) {
            builder.addDataList(toProto(e));
        }
        return builder.build();
    }

    public static Msgother.PB_SCMysteryShopInfo toMysteryInfo(MysteryShopStateEntity e) {
        Msgother.PB_SCMysteryShopInfo.Builder builder = Msgother.PB_SCMysteryShopInfo.newBuilder()
                .setBuyFlag(e.getBuyFlag());
        if (e.getIndexList() != null) {
            for (String s : e.getIndexList().split(",")) {
                builder.addIndexList(Integer.parseInt(s));
            }
        }
        return builder.build();
    }

}