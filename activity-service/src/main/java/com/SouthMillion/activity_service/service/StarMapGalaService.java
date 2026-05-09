package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.StarMapGala;
import com.SouthMillion.activity_service.repository.StarMapGalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StarMapGalaService {
    
    @Autowired
    private StarMapGalaRepository starMapGalaRepository;

    public Map<Long, StarMapGala> getGalaByRoleIds(List<Long> roleIds) {
        List<StarMapGala> galas = starMapGalaRepository.findByRoleIdsIn(roleIds);
        Map<Long, StarMapGala> galaMap = new HashMap<>();
        for (StarMapGala gala : galas) {
            galaMap.put(gala.getRoleId(), gala);
        }
        return galaMap;
    }
}