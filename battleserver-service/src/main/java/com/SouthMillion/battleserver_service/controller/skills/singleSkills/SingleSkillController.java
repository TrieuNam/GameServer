package com.SouthMillion.battleserver_service.controller.skills.singleSkills;

import com.SouthMillion.battleserver_service.service.skills.singleSkills.SingleSkillService;
import org.SouthMillion.dto.SingleSkillDTO;
import org.SouthMillion.dto.SingleSkillEffectDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/single_skill")
public class SingleSkillController {

    @Autowired
    private SingleSkillService singleSkillService;

    @GetMapping("/eff")
    public List<SingleSkillEffectDTO> getAllSkillEff() {
        return singleSkillService.getAllSkillEff();
    }

    @GetMapping("/eff/{seq}")
    public SingleSkillEffectDTO getSkillEffBySeq(@PathVariable String seq) {
        return singleSkillService.getSkillEffBySeq(seq);
    }

    @GetMapping("/cfg")
    public List<SingleSkillDTO> getAllSkillCfg() {
        return singleSkillService.getAllSkillCfg();
    }

    @GetMapping("/cfg/{skillId}")
    public SingleSkillDTO getSkillCfgBySkillId(@PathVariable String skillId) {
        return singleSkillService.getSkillCfgBySkillId(skillId);
    }
}