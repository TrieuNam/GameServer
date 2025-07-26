package org.SouthMillion.mapper.user;

import org.SouthMillion.dto.user.*;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;

public class RoleProtoMapper {
    // 1. RoleDTO -> PB_SCRoleInfoAck
    public static Msgrole.PB_SCRoleInfoAck toProto(RoleDTO dto) {
        Msgrole.PB_SCRoleInfoAck.Builder builder = Msgrole.PB_SCRoleInfoAck.newBuilder();
        if (dto.getCurExp() != null) builder.setCurExp(dto.getCurExp());
        if (dto.getCreateTime() != null) builder.setCreateTime(dto.getCreateTime());
        if (dto.getRoleinfo() != null) builder.setRoleinfo(toProtoRoleInfo(dto.getRoleinfo()));
        if (dto.getAppearance() != null) builder.setAppearance(toProtoAppearance(dto.getAppearance()));
        return builder.build();
    }

    // 2. RoleInfoDTO -> PB_RoleInfo
    public static Msgrole.PB_RoleInfo toProtoRoleInfo(RoleInfoDTO dto) {
        Msgrole.PB_RoleInfo.Builder builder = Msgrole.PB_RoleInfo.newBuilder();
        if (dto.getRoleId() != null) builder.setRoleId(dto.getRoleId().intValue());  // int32
        if (dto.getName() != null) builder.setName(com.google.protobuf.ByteString.copyFromUtf8(dto.getName())); // bytes
        if (dto.getLevel() != null) builder.setLevel(dto.getLevel());  // int32
        if (dto.getCap() != null) builder.setCap(dto.getCap());        // int64
        if (dto.getHeadPicId() != null) builder.setHeadPicId(dto.getHeadPicId());
        if (dto.getTitleId() != null) builder.setTitleId(dto.getTitleId());
        if (dto.getGuildName() != null) builder.setGuildName(com.google.protobuf.ByteString.copyFromUtf8(dto.getGuildName()));
        if (dto.getKnightLevel() != null) builder.setKnightLevel(dto.getKnightLevel());
        if (dto.getHeadChar() != null) builder.setHeadChar(com.google.protobuf.ByteString.copyFromUtf8(dto.getHeadChar()));
        return builder.build();
    }

    // 3. AppearanceDTO -> PB_Appearance
    public static Msgrole.PB_Appearance toProtoAppearance(AppearanceDTO dto) {
        Msgrole.PB_Appearance.Builder builder = Msgrole.PB_Appearance.newBuilder();
        if (dto.getSurfaceWeapon() != null) builder.setSurfaceWeapon(dto.getSurfaceWeapon());
        if (dto.getSurfaceShield() != null) builder.setSurfaceShield(dto.getSurfaceShield());
        if (dto.getSurfaceBody() != null) builder.setSurfaceBody(dto.getSurfaceBody());
        if (dto.getSurfaceMount() != null) builder.setSurfaceMount(dto.getSurfaceMount());
        if (dto.getSurfaceHead() != null) builder.setSurfaceHead(dto.getSurfaceHead());
        if (dto.getSurfaceAngel() != null) builder.setSurfaceAngel(dto.getSurfaceAngel());
        return builder.build();
    }

    public static Msgrole.PB_SCRoleAttrList toProto(RoleAttrListDTO dto) {
        Msgrole.PB_SCRoleAttrList.Builder builder = Msgrole.PB_SCRoleAttrList.newBuilder();
        if (dto.getNotifyReason() != null) builder.setNotifyReason(dto.getNotifyReason());
        if (dto.getCapability() != null) builder.setCapability(dto.getCapability());
        if (dto.getAttrList() != null) {
            for (AttrPairDTO attr : dto.getAttrList()) {
                builder.addAttrList(toProto(attr));
            }
        }
        return builder.build();
    }

    public static Msgrole.PB_AttrPair toProto(AttrPairDTO dto) {
        Msgrole.PB_AttrPair.Builder builder = Msgrole.PB_AttrPair.newBuilder();
        if (dto.getAttrType() != null) builder.setAttrType(dto.getAttrType());
        if (dto.getAttrValue() != null) builder.setAttrValue(dto.getAttrValue());
        return builder.build();
    }

    public static Msgrole.PB_SCRoleLevelChange toProto(RoleLevelChangeDTO dto) {
        Msgrole.PB_SCRoleLevelChange.Builder builder = Msgrole.PB_SCRoleLevelChange.newBuilder();
        if (dto.getLevel() != null) builder.setLevel(dto.getLevel());
        if (dto.getExp() != null) builder.setExp(dto.getExp());
        return builder.build();
    }

    public static Msgrole.PB_SCGetOtherRoleRet toProtoOtherRole(RoleDTO dto) {
        Msgrole.PB_SCGetOtherRoleRet.Builder builder = Msgrole.PB_SCGetOtherRoleRet.newBuilder();
        if (dto.getUid() != null) builder.setUid(dto.getUid());
        if (dto.getRoleinfo() != null) builder.setRoleinfo(toProtoRoleInfo(dto.getRoleinfo()));
        if (dto.getAppearance() != null) builder.setAppearance(toProtoAppearance(dto.getAppearance()));
        if (dto.getRoleAttrList() != null) {
            for (AttrPairDTO attr : dto.getRoleAttrList()) {
                builder.addRoleAttrList(toProtoAttrPair(attr));
            }
        }
        if (dto.getPetList() != null) {
            for (SCOtherPetDTO pet : dto.getPetList()) {
                builder.addPetList(toProtoOtherPet(pet));
            }
        }
        if (dto.getEquipList() != null) {
            for (EquipDataDTO equip : dto.getEquipList()) {
                builder.addEquipList(toProtoEquipData(equip));
            }
        }
        if (dto.getAngelAppearance() != null) builder.setAngelAppearance(dto.getAngelAppearance());
        if (dto.getStarMapLevel() != null) builder.setStarMapLevel(dto.getStarMapLevel());
        if (dto.getGemLevel() != null) builder.setGemLevel(dto.getGemLevel());
        return builder.build();
    }

    public static Msgother.PB_SCLimitCoreInfo toProto(LimitCoreInfoDTO dto) {
        Msgother.PB_SCLimitCoreInfo.Builder builder = Msgother.PB_SCLimitCoreInfo.newBuilder();
        if (dto.getCoreLevel() != null) {
            builder.addAllCoreLevel(dto.getCoreLevel());
        }
        return builder.build();
    }

    public static SystemSetDTO fromProto(Msgrole.PB_system_set proto) {
        SystemSetDTO dto = new SystemSetDTO();
        if (proto.hasSystemSetType()) dto.setSystemSetType(proto.getSystemSetType());
        if (proto.hasSystemSetParam()) dto.setSystemSetParam(proto.getSystemSetParam());
        return dto;
    }

    public static Msgrole.PB_AttrPair toProtoAttrPair(AttrPairDTO dto) {
        Msgrole.PB_AttrPair.Builder builder = Msgrole.PB_AttrPair.newBuilder();
        if (dto.getAttrType() != null) builder.setAttrType(dto.getAttrType());
        if (dto.getAttrValue() != null) builder.setAttrValue(dto.getAttrValue());
        return builder.build();
    }

    public static Msgrole.PB_SCOtherPet toProtoOtherPet(SCOtherPetDTO dto) {
        Msgrole.PB_SCOtherPet.Builder builder = Msgrole.PB_SCOtherPet.newBuilder();
        if (dto.getPetId() != null) builder.setPetId(dto.getPetId());
        if (dto.getPetLevel() != null) builder.setPetLevel(dto.getPetLevel());
        if (dto.getPetOrder() != null) builder.setPetOrder(dto.getPetOrder());
        if (dto.getPetAttrList() != null) {
            for (AttrPairDTO attr : dto.getPetAttrList()) {
                builder.addPetAttrList(toProtoAttrPair(attr));
            }
        }
        if (dto.getPetCap() != null) builder.setPetCap(dto.getPetCap());
        return builder.build();
    }

    public static Msgequip.PB_EquipData toProtoEquipData(EquipDataDTO dto) {
        Msgequip.PB_EquipData.Builder builder = Msgequip.PB_EquipData.newBuilder();
        if (dto.getEquipType() != null) builder.setEquipType(dto.getEquipType());
        if (dto.getItemId() != null) builder.setItemId(dto.getItemId());
        if (dto.getHp() != null) builder.setHp(dto.getHp());
        if (dto.getAttack() != null) builder.setAttack(dto.getAttack());
        if (dto.getDefend() != null) builder.setDefend(dto.getDefend());
        if (dto.getSpeed() != null) builder.setSpeed(dto.getSpeed());
        if (dto.getAttrType1() != null) builder.setAttrType1(dto.getAttrType1());
        if (dto.getAttrValue1() != null) builder.setAttrValue1(dto.getAttrValue1());
        if (dto.getAttrType2() != null) builder.setAttrType2(dto.getAttrType2());
        if (dto.getAttrValue2() != null) builder.setAttrValue2(dto.getAttrValue2());
        return builder.build();
    }

    public static Msgrole.PB_SCRoleSystemSetInfo toProto(RoleSystemSetDTO dto) {
        Msgrole.PB_SCRoleSystemSetInfo.Builder builder = Msgrole.PB_SCRoleSystemSetInfo.newBuilder();
        if (dto.getSystemSetList() != null) {
            for (SystemSetDTO s : dto.getSystemSetList()) {
                builder.addSystemSetList(toProto(s));
            }
        }
        return builder.build();
    }

    // Mapping DTO -> Proto (PB_system_set)
    public static Msgrole.PB_system_set toProto(SystemSetDTO dto) {
        Msgrole.PB_system_set.Builder builder = Msgrole.PB_system_set.newBuilder();
        if (dto.getSystemSetType() != null) builder.setSystemSetType(dto.getSystemSetType());
        if (dto.getSystemSetParam() != null) builder.setSystemSetParam(dto.getSystemSetParam());
        return builder.build();
    }

}
