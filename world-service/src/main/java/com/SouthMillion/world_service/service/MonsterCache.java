package com.SouthMillion.world_service.service;


import com.SouthMillion.world_service.service.client.ConfigServiceClient;
import jakarta.annotation.PostConstruct;
import org.SouthMillion.dto.battle.MonsterDTO;
import org.SouthMillion.dto.battle.MonsterGroupDTO;
import org.SouthMillion.dto.battle.MonsterInstanceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonsterCache {
    @Autowired
    ConfigServiceClient configFeign;

    private Map<String, MonsterInstanceDTO> instanceMap = new ConcurrentHashMap<>();
    private List<MonsterDTO> monsters;
    private List<MonsterGroupDTO> groups;

    @PostConstruct
    public void load() {
        monsters = configFeign.getMonsters();
        groups = configFeign.getGroups();
        for (MonsterGroupDTO group : groups) {
            for (String monsterId : group.getMonsterIds()) {
                MonsterDTO m = monsters.stream().filter(mon -> mon.getMonsterId().equals(monsterId)).findFirst().orElse(null);
                if (m != null) {
                    String instId = UUID.randomUUID().toString();
                    MonsterInstanceDTO mi = new MonsterInstanceDTO();
                    mi.setInstanceId(instId);
                    mi.setMonster(m);
                    mi.setHp(Integer.parseInt(m.getHp()));
                    mi.setAttack(Integer.parseInt(m.getAttack()));
                    mi.setDefense(Integer.parseInt(m.getDefense()));
                    mi.setSpeed(Integer.parseInt(m.getSpeed()));
                    mi.setAlive(true);
                    // parse skillIds/passiveSkillIds
                    mi.setSkillIds(Arrays.asList(m.getSkillId().split(",")));
                    mi.setPassiveSkillIds(Arrays.asList(m.getPassiveSkillId().split(",")));
                    mi.setActiveBuffs(new ArrayList<>());
                    mi.setState("normal");
                    instanceMap.put(instId, mi);
                }
            }
        }
    }

    public MonsterInstanceDTO getAliveInstance(String instanceId) {
        MonsterInstanceDTO mi = instanceMap.get(instanceId);
        return (mi != null && mi.isAlive()) ? mi : null;
    }

    public void setDead(String instanceId) {
        MonsterInstanceDTO mi = instanceMap.get(instanceId);
        if (mi != null) mi.setAlive(false);
    }

    public List<MonsterInstanceDTO> getAllAlive() {
        List<MonsterInstanceDTO> list = new ArrayList<>();
        for (MonsterInstanceDTO mi : instanceMap.values()) if (mi.isAlive()) list.add(mi);
        return list;
    }
}
