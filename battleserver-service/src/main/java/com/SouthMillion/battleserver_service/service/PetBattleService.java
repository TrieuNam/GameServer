package com.SouthMillion.battleserver_service.service;

import com.SouthMillion.battleserver_service.service.serviceClient.PetFeignClient;
import org.SouthMillion.dto.pet.PetBattleResultDTO;
import org.SouthMillion.dto.pet.PetStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class PetBattleService {
    @Autowired
    private PetFeignClient petFeignClient;

    public PetBattleResultDTO battle(int petIdA, Integer itemA, Integer weaponA, int petIdB, Integer itemB, Integer weaponB) {
        PetStatsDTO statsA = petFeignClient.getPetStats(petIdA, itemA, weaponA);
        PetStatsDTO statsB = petFeignClient.getPetStats(petIdB, itemB, weaponB);

        // Battle logic y hệt trước đây
        int hpA = statsA.getHp(), hpB = statsB.getHp();
        int atkA = statsA.getAtk(), atkB = statsB.getAtk();
        int defA = statsA.getDef(), defB = statsB.getDef();
        int speedA = statsA.getSpeed(), speedB = statsB.getSpeed();

        boolean aFirst = speedA >= speedB;
        Random random = new Random();
        int round = 0;
        while (hpA > 0 && hpB > 0 && round < 99) {
            if (aFirst) {
                hpB -= Math.max(1, atkA - defB + random.nextInt(10));
                if (hpB <= 0) break;
                hpA -= Math.max(1, atkB - defA + random.nextInt(10));
            } else {
                hpA -= Math.max(1, atkB - defA + random.nextInt(10));
                if (hpA <= 0) break;
                hpB -= Math.max(1, atkA - defB + random.nextInt(10));
            }
            round++;
        }
        String winner;
        if (hpA > 0 && hpB <= 0) winner = "A";
        else if (hpB > 0 && hpA <= 0) winner = "B";
        else if (hpA > hpB) winner = "A";
        else if (hpB > hpA) winner = "B";
        else winner = "Draw";
        return new PetBattleResultDTO(winner, Math.max(hpA, 0), Math.max(hpB, 0), round);
    }
}