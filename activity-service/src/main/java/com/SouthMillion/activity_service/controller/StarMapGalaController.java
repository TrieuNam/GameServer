package com.SouthMillion.activity_service.controller;

import com.SouthMillion.activity_service.service.StarMapGalaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/star-map")
public class StarMapGalaController {

    @Autowired
    private StarMapGalaService starMapGalaService;

    @GetMapping("/gala")
    public Map<Long, ? extends Object> getGalaByRoleIds(@RequestParam List<Long> roleIds) {
        return starMapGalaService.getGalaByRoleIds(roleIds);
    }
}