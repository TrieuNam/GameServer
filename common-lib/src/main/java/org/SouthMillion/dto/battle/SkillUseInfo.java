package org.SouthMillion.dto.battle;

import lombok.Data;

import java.util.List;

@Data
public class SkillUseInfo {
    private String skillId;
    private SingleSkillDTO skillDTO;
    private List<SingleSkillEffectDTO> effects; // Có thể nhiều effect (vd: dame + stun)
}