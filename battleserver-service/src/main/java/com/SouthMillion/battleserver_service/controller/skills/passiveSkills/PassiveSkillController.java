package com.SouthMillion.battleserver_service.controller.skills.passiveSkills;

import com.SouthMillion.battleserver_service.service.skills.passiveSkills.PassiveSkillService;
import org.SouthMillion.dto.PassiveSkillDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/passive_skill")
public class PassiveSkillController {

    @Autowired
    private PassiveSkillService passiveSkillService;

    @GetMapping
    public List<PassiveSkillDTO> getAllPassiveCfg() {
        return passiveSkillService.getAllPassiveCfg();
    }

    @GetMapping("/{skillId}")
    public PassiveSkillDTO getPassiveCfgBySkillId(@PathVariable String skillId) {
        return passiveSkillService.getPassiveCfgBySkillId(skillId);
    }
}