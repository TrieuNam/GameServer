package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
@Setter
@Getter
public class SCOtherPetDTO implements Serializable {
    private Integer petId;                   // 宠物id
    private Integer petLevel;                // 宠物等级
    private Integer petOrder;                // 宠物阶级
    private List<AttrPairDTO> petAttrList;   // 宠物属性列表
    private Long petCap;                     // 宠物战力
}