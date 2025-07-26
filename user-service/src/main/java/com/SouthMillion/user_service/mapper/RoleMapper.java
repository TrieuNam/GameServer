package com.SouthMillion.user_service.mapper;

import com.SouthMillion.user_service.enity.RoleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import org.SouthMillion.dto.user.*;
import org.SouthMillion.proto.Msgrole.Msgrole;

import java.util.List;

public class RoleMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static RoleDTO fromEntity(RoleEntity e) {
        RoleDTO dto = new RoleDTO();
        dto.setCurExp(e.getCurExp());
        dto.setCreateTime(e.getCreateTime());
        dto.setUid(e.getUid() != null ? e.getUid().intValue() : null); // chuẩn hóa uid sang Integer (nếu cần)

        try {
            if (e.getRoleinfoJson() != null)
                dto.setRoleinfo(objectMapper.readValue(e.getRoleinfoJson(), RoleInfoDTO.class));
        } catch (Exception ex) { ex.printStackTrace(); }
        try {
            if (e.getAppearanceJson() != null)
                dto.setAppearance(objectMapper.readValue(e.getAppearanceJson(), AppearanceDTO.class));
        } catch (Exception ex) { ex.printStackTrace(); }
        try {
            if (e.getAttrListJson() != null)
                dto.setRoleAttrList(objectMapper.readValue(e.getAttrListJson(), objectMapper.getTypeFactory().constructCollectionType(List.class, AttrPairDTO.class)));
        } catch (Exception ex) { ex.printStackTrace(); }
        try {
            if (e.getPetListJson() != null)
                dto.setPetList(objectMapper.readValue(e.getPetListJson(), objectMapper.getTypeFactory().constructCollectionType(List.class, SCOtherPetDTO.class)));
        } catch (Exception ex) { ex.printStackTrace(); }
        try {
            if (e.getEquipListJson() != null)
                dto.setEquipList(objectMapper.readValue(e.getEquipListJson(), objectMapper.getTypeFactory().constructCollectionType(List.class, EquipDataDTO.class)));
        } catch (Exception ex) { ex.printStackTrace(); }

        if (e.getAngelAppearance() != null)
            dto.setAngelAppearance(e.getAngelAppearance());
        if (e.getStarMapLevel() != null)
            dto.setStarMapLevel(e.getStarMapLevel());
        if (e.getGemLevel() != null)
            dto.setGemLevel(e.getGemLevel());

        return dto;
    }

}
