package org.SouthMillion.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RoleDTO implements Serializable {
    private Long curExp;
    private Long createTime;
    private RoleInfoDTO roleinfo;
    private AppearanceDTO appearance;

    // --- Các field bổ sung cho 1463/RoleInfo ---
    private Integer uid;
    private List<AttrPairDTO> roleAttrList;
    private List<SCOtherPetDTO> petList;
    private List<EquipDataDTO> equipList;
    private Integer angelAppearance;
    private Integer starMapLevel;
    private Integer gemLevel;
}