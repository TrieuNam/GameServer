package com.example.ArenaService.controller;

import com.example.ArenaService.dto.arena.ArenaCfgDTO;
import com.example.ArenaService.dto.arena.AwardDTO;
import com.example.ArenaService.dto.arena.MonsterDTO;
import com.example.ArenaService.dto.arena.WeeklyJoinAwardDTO;
import com.example.ArenaService.service.arena.ArenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/arena")
public class ArenaController {

    @Autowired
    private ArenaService arenaService;

    @GetMapping("/arena_cfg")
    public List<ArenaCfgDTO> getArenaCfgList() throws Exception {
        return arenaService.getArenaCfgList();
    }
    @GetMapping("/arena_cfg/{initialNum}")
    public ArenaCfgDTO getArenaCfgByInitialNum(@PathVariable String initialNum) throws Exception {
        return arenaService.getArenaCfgByInitialNum(initialNum);
    }

    @GetMapping("/everyday_award")
    public List<AwardDTO> getEverydayAwardList() throws Exception {
        return arenaService.getEverydayAwardList();
    }
    @GetMapping("/everyday_award/{seq}")
    public AwardDTO getEverydayAwardBySeq(@PathVariable String seq) throws Exception {
        return arenaService.getEverydayAwardBySeq(seq);
    }

    @GetMapping("/weekly_award")
    public List<AwardDTO> getWeeklyAwardList() throws Exception {
        return arenaService.getWeeklyAwardList();
    }
    @GetMapping("/weekly_award/{seq}")
    public AwardDTO getWeeklyAwardBySeq(@PathVariable String seq) throws Exception {
        return arenaService.getWeeklyAwardBySeq(seq);
    }

    @GetMapping("/weekly_join_award")
    public List<WeeklyJoinAwardDTO> getWeeklyJoinAwardList() throws Exception {
        return arenaService.getWeeklyJoinAwardList();
    }
    @GetMapping("/weekly_join_award/{seq}")
    public WeeklyJoinAwardDTO getWeeklyJoinAwardBySeq(@PathVariable String seq) throws Exception {
        return arenaService.getWeeklyJoinAwardBySeq(seq);
    }

    @GetMapping("/arena_monster")
    public List<MonsterDTO> getArenaMonsterList() throws Exception {
        return arenaService.getArenaMonsterList();
    }
    @GetMapping("/arena_monster/{seq}")
    public MonsterDTO getArenaMonsterBySeq(@PathVariable String seq) throws Exception {
        return arenaService.getArenaMonsterBySeq(seq);
    }
}
