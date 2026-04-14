package com.SouthMillion.webSocket_server.service.pet;


import com.SouthMillion.webSocket_server.service.client.PetGuardFeign;
import com.SouthMillion.webSocket_server.service.client.BattleFeign;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import org.SouthMillion.dto.bag.BagDTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import petguard_service.PetguardService.PB_CSPetFbReq;
import petguard_service.PetguardService.PB_SCPetFbInfo;
// Đã xoá import PetGuardState vì không còn entity.
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class PetGuardService {
    private final PetGuardFeign petGuardFeign;
    private final BattleFeign battleFeign;
    private final BagFeign bagFeign;

    public PB_SCPetFbInfo process(PB_CSPetFbReq req, Long roleId) {

        PB_SCPetFbInfo.Builder resp = PB_SCPetFbInfo.newBuilder();
        if (roleId == null || roleId <= 0) {
            resp.setMessage("Thiếu roleId hoặc không hợp lệ");
            return resp.build();
        }

        // Lấy state từ pet-service qua Feign (dạng Map)
        Object stateObj = petGuardFeign.getState(roleId);
        Map<String, Object> state = (stateObj instanceof Map) ? (Map<String, Object>) stateObj : new HashMap<>();
        int currentPassLevel = ((Number) state.getOrDefault("passLevel", 0)).intValue();
        long fetchFlag = ((Number) state.getOrDefault("fetchFlag", 0L)).longValue();

        if (req.getType() == 1) { // FIGHT
            int level = req.getP1();
            if (level != currentPassLevel + 1) {
                resp.setMessage("Level không hợp lệ hoặc đã vượt qua");
                resp.setPassLevel(currentPassLevel);
                resp.setFetchFlag(fetchFlag);
                return resp.build();
            }
            Map<String, Object> battleResult = battleFeign.startPetFbBattle(roleId, level);
            boolean win = battleResult.getOrDefault("win", true).equals(Boolean.TRUE);
            if (win) {
                currentPassLevel = level;
                // Đã xoá code liên quan đến PetGuardState vì không còn entity.
                resp.setPassLevel(currentPassLevel);
                resp.setFetchFlag(fetchFlag);
                resp.setMessage("Chiến đấu thành công, đã vượt qua level " + level);
            } else {
                resp.setPassLevel(currentPassLevel);
                resp.setFetchFlag(fetchFlag);
                resp.setMessage("Thua trận, chưa vượt qua level " + level);
            }
        } else if (req.getType() == 2) { // REWARD
            int level = req.getP1();
            if (level > currentPassLevel) {
                resp.setMessage("Chưa vượt qua level này");
                resp.setPassLevel(currentPassLevel);
                resp.setFetchFlag(fetchFlag);
                return resp.build();
            }
            if (((fetchFlag >> level) & 1) == 1) {
                resp.setMessage("Đã nhận thưởng level này rồi");
                resp.setPassLevel(currentPassLevel);
                resp.setFetchFlag(fetchFlag);
                return resp.build();
            }
            Map<Integer, Long> rewards = new HashMap<>();
            rewards.put(1001, 10L);
            BagDTOs.GrantReq grantReq = new BagDTOs.GrantReq();
            grantReq.setRoleId(String.valueOf(roleId));
            List<BagDTOs.GrantItem> grantItems = rewards.entrySet().stream()
                    .map(e -> BagDTOs.GrantItem.builder()
                            .itemId(e.getKey())
                            .num(e.getValue().intValue())
                            .build())
                    .toList();
            grantReq.setItems(grantItems);
            bagFeign.grant(grantReq);
            resp.putAllRewards(rewards);
            fetchFlag |= (1L << level);
            // Đã xoá code liên quan đến PetGuardState vì không còn entity.
            resp.setPassLevel(currentPassLevel);
            resp.setFetchFlag(fetchFlag);
            resp.setMessage("Nhận thưởng thành công cho level " + level);
        } else {
            resp.setMessage("Loại yêu cầu không hợp lệ");
        }
        return resp.build();
    }
}
