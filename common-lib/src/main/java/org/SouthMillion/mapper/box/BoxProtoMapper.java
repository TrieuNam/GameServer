package org.SouthMillion.mapper.box;

import org.SouthMillion.dto.item.Box.BoxInfoDTO;
import org.SouthMillion.dto.item.Box.BoxOpenRequestDTO;
import org.SouthMillion.dto.item.Box.BoxRecordDTO;
import org.SouthMillion.dto.item.Box.BoxSetDTO;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgequip.Msgequip;

import java.util.HashMap;

public class BoxProtoMapper {


    public static Msgbox.PB_SCBoxInfo toProto(BoxInfoDTO dto) {
        Msgbox.PB_SCBoxInfo.Builder builder = Msgbox.PB_SCBoxInfo.newBuilder();
        if (dto.getBoxLevel() != null) builder.setBoxLevel(dto.getBoxLevel());
        if (dto.getBuyTimes() != null) builder.setBuyTimes(dto.getBuyTimes());
        if (dto.getTimestamp() != null) builder.setTimestamp(dto.getTimestamp().intValue());
        if (dto.getArenaItemNum() != null) builder.setArenaItemNum(dto.getArenaItemNum());
        if (dto.getShiZhuangNum() != null) builder.setShiZhuangNum(dto.getShiZhuangNum());
        if (dto.getLevelFetchFlag() != null) builder.setLevelFetchFlag(dto.getLevelFetchFlag());
        return builder.build();
    }

    public static BoxOpenRequestDTO toBoxOpenRequestDTO(Msgbox.PB_CSBoxReq req, Long userId) {
        BoxOpenRequestDTO dto = new BoxOpenRequestDTO();
        dto.setUserId(userId);

        String boxType;
        switch (req.getReqType()) {
            case 1: boxType = "unpack"; break;
            case 2: boxType = "equip"; break;
            case 3: boxType = "sell"; break;
            default: boxType = "other";
        }
        dto.setBoxType(boxType);
        dto.setBoxId(req.hasParam() ? req.getParam() : null);
        dto.setExtraParams(new HashMap<>()); // Add extra params if needed
        return dto;
    }

    public static BoxSetDTO toBoxSetDTO(Msgbox.PB_BoxSet pbBoxSet) {
        BoxSetDTO dto = new BoxSetDTO();
        dto.setEquipEqality(pbBoxSet.hasEquipEqality() ? pbBoxSet.getEquipEqality() : null);
        dto.setConditionFirstMark(pbBoxSet.hasConditionFirstMark() ? pbBoxSet.getConditionFirstMark() : null);
        dto.setConditionFirst1(pbBoxSet.hasConditionFirst1() ? pbBoxSet.getConditionFirst1() : null);
        dto.setConditionFirst2(pbBoxSet.hasConditionFirst2() ? pbBoxSet.getConditionFirst2() : null);
        dto.setConditionSecondMark(pbBoxSet.hasConditionSecondMark() ? pbBoxSet.getConditionSecondMark() : null);
        dto.setConditionSecond1(pbBoxSet.hasConditionSecond1() ? pbBoxSet.getConditionSecond1() : null);
        dto.setConditionSecond2(pbBoxSet.hasConditionSecond2() ? pbBoxSet.getConditionSecond2() : null);
        dto.setRetainMark(pbBoxSet.hasRetainMark() ? pbBoxSet.getRetainMark() : null);
        dto.setChallengeMark(pbBoxSet.hasChallengeMark() ? pbBoxSet.getChallengeMark() : null);
        dto.setEquipCapMark(pbBoxSet.hasEquipCapMark() ? pbBoxSet.getEquipCapMark() : null);
        dto.setEquipSellMark(pbBoxSet.hasEquipSellMark() ? pbBoxSet.getEquipSellMark() : null);
        dto.setOpenFiveMark(pbBoxSet.hasOpenFiveMark() ? pbBoxSet.getOpenFiveMark() : null);
        return dto;
    }

    public static Msgbox.PB_SCBoxEquipInfo toProtoBoxEquipInfo(BoxRecordDTO record) {
        Msgbox.PB_SCBoxEquipInfo.Builder builder = Msgbox.PB_SCBoxEquipInfo.newBuilder();
        builder.setIsNew(1); // hoặc 0 tuỳ nghiệp vụ

        Msgequip.PB_EquipData.Builder equipBuilder = Msgequip.PB_EquipData.newBuilder();

        if (record.getEquipType() != null)
            equipBuilder.setEquipType(record.getEquipType());
        if (record.getRewardItemId() != null)
            equipBuilder.setItemId(record.getRewardItemId());
        if (record.getHp() != null)
            equipBuilder.setHp(record.getHp());
        if (record.getAttack() != null)
            equipBuilder.setAttack(record.getAttack());
        if (record.getDefend() != null)
            equipBuilder.setDefend(record.getDefend());
        if (record.getSpeed() != null)
            equipBuilder.setSpeed(record.getSpeed());
        if (record.getAttrType1() != null)
            equipBuilder.setAttrType1(record.getAttrType1());
        if (record.getAttrValue1() != null)
            equipBuilder.setAttrValue1(record.getAttrValue1());
        if (record.getAttrType2() != null)
            equipBuilder.setAttrType2(record.getAttrType2());
        if (record.getAttrValue2() != null)
            equipBuilder.setAttrValue2(record.getAttrValue2());

        builder.setEquipInfo(equipBuilder);

        return builder.build();
    }

    public static Msgbox.PB_SCBoxInfo toProtoBoxInfo(BoxRecordDTO record) {
        Msgbox.PB_SCBoxInfo.Builder builder = Msgbox.PB_SCBoxInfo.newBuilder();
        if (record.getBoxId() != null)
            builder.setBoxLevel(record.getBoxId());
        if (record.getTimestamp() != null)
            builder.setTimestamp(record.getTimestamp());
        // mapping thêm các trường khác nếu proto có
        return builder.build();
    }

    public static Msgbox.PB_SCBoxSetingInfo toProtoBoxSetingInfo(BoxSetDTO setting) {
        Msgbox.PB_BoxSet.Builder boxSetBuilder = Msgbox.PB_BoxSet.newBuilder();
        if (setting.getEquipEqality() != null)
            boxSetBuilder.setEquipEqality(setting.getEquipEqality());
        if (setting.getConditionFirstMark() != null)
            boxSetBuilder.setConditionFirstMark(setting.getConditionFirstMark());
        if (setting.getConditionFirst1() != null)
            boxSetBuilder.setConditionFirst1(setting.getConditionFirst1());
        if (setting.getConditionFirst2() != null)
            boxSetBuilder.setConditionFirst2(setting.getConditionFirst2());
        if (setting.getConditionSecondMark() != null)
            boxSetBuilder.setConditionSecondMark(setting.getConditionSecondMark());
        if (setting.getConditionSecond1() != null)
            boxSetBuilder.setConditionSecond1(setting.getConditionSecond1());
        if (setting.getConditionSecond2() != null)
            boxSetBuilder.setConditionSecond2(setting.getConditionSecond2());
        if (setting.getRetainMark() != null)
            boxSetBuilder.setRetainMark(setting.getRetainMark());
        if (setting.getChallengeMark() != null)
            boxSetBuilder.setChallengeMark(setting.getChallengeMark());
        if (setting.getEquipCapMark() != null)
            boxSetBuilder.setEquipCapMark(setting.getEquipCapMark());
        if (setting.getEquipSellMark() != null)
            boxSetBuilder.setEquipSellMark(setting.getEquipSellMark());
        if (setting.getOpenFiveMark() != null)
            boxSetBuilder.setOpenFiveMark(setting.getOpenFiveMark());

        Msgbox.PB_SCBoxSetingInfo.Builder builder = Msgbox.PB_SCBoxSetingInfo.newBuilder();
        builder.setBoxSet(boxSetBuilder);
        return builder.build();
    }

    public static Msgbox.PB_SCBoxSellInfo toProtoBoxSellInfo(BoxRecordDTO record) {
        Msgbox.PB_SCBoxSellInfo.Builder builder = Msgbox.PB_SCBoxSellInfo.newBuilder();
        builder.setSellCoin(record.getSellPrice() == null ? 0 : record.getSellPrice());
        builder.setSellExp(record.getExp() == null ? 0 : record.getExp());
        return builder.build();
    }

    public static Msgbox.PB_BoxSet toProtoBoxSet(BoxSetDTO setting) {
        Msgbox.PB_BoxSet.Builder builder = Msgbox.PB_BoxSet.newBuilder();
        if (setting.getEquipEqality() != null)
            builder.setEquipEqality(setting.getEquipEqality());
        if (setting.getConditionFirstMark() != null)
            builder.setConditionFirstMark(setting.getConditionFirstMark());
        if (setting.getConditionFirst1() != null)
            builder.setConditionFirst1(setting.getConditionFirst1());
        if (setting.getConditionFirst2() != null)
            builder.setConditionFirst2(setting.getConditionFirst2());
        if (setting.getConditionSecondMark() != null)
            builder.setConditionSecondMark(setting.getConditionSecondMark());
        if (setting.getConditionSecond1() != null)
            builder.setConditionSecond1(setting.getConditionSecond1());
        if (setting.getConditionSecond2() != null)
            builder.setConditionSecond2(setting.getConditionSecond2());
        if (setting.getRetainMark() != null)
            builder.setRetainMark(setting.getRetainMark());
        if (setting.getChallengeMark() != null)
            builder.setChallengeMark(setting.getChallengeMark());
        if (setting.getEquipCapMark() != null)
            builder.setEquipCapMark(setting.getEquipCapMark());
        if (setting.getEquipSellMark() != null)
            builder.setEquipSellMark(setting.getEquipSellMark());
        if (setting.getOpenFiveMark() != null)
            builder.setOpenFiveMark(setting.getOpenFiveMark());
        return builder.build();
    }
}
