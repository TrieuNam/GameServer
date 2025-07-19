package com.example.ArenaService.controller;

import com.example.ArenaService.dto.arena.AwardDTO;
import com.example.ArenaService.dto.arena.DfArenaCfgDTO;
import com.example.ArenaService.service.arena.DfArenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/df-arena")
public class DfArenaController {

    @Autowired
    private DfArenaService dfArenaService;

    @GetMapping("/cfg")
    public List<DfArenaCfgDTO> getDfArenaCfgList() throws Exception {
        return dfArenaService.getDfArenaCfgList();
    }

    @GetMapping("/cfg/{initialNum}")
    public DfArenaCfgDTO getDfArenaCfgByInitialNum(@PathVariable String initialNum) throws Exception {
        return dfArenaService.getDfArenaCfgByInitialNum(initialNum);
    }

    @GetMapping("/everyday_award")
    public List<AwardDTO> getDfEverydayAwardList() throws Exception {
        return dfArenaService.getDfEverydayAwardList();
    }

    @GetMapping("/everyday_award/{seq}")
    public AwardDTO getDfEverydayAwardBySeq(@PathVariable String seq) throws Exception {
        return dfArenaService.getDfEverydayAwardBySeq(seq);
    }

    @GetMapping("/weekly_award")
    public List<AwardDTO> getDfWeeklyAwardList() throws Exception {
        return dfArenaService.getDfWeeklyAwardList();
    }

    @GetMapping("/weekly_award/{seq}")
    public AwardDTO getDfWeeklyAwardBySeq(@PathVariable String seq) throws Exception {
        return dfArenaService.getDfWeeklyAwardBySeq(seq);
    }
}