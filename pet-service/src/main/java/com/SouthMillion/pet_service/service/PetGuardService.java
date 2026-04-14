package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.model.entity.PetGuardState;
import com.SouthMillion.pet_service.repository.PetGuardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PetGuardService {
    private final PetGuardRepository petGuardRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String PASS_LEVEL_KEY = "petguard:passLevel:";
    public static final String FETCH_FLAG_KEY = "petguard:fetchFlag:";

    @Transactional
    public PetGuardState getOrInitState(Long roleId) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        String passLevelKey = PASS_LEVEL_KEY + roleId;
        String fetchFlagKey = FETCH_FLAG_KEY + roleId;
        Integer passLevel = (Integer) ops.get(passLevelKey);
        Long fetchFlag = (Long) ops.get(fetchFlagKey);
        if (passLevel == null || fetchFlag == null) {
            Optional<PetGuardState> stateOpt = petGuardRepository.findByRoleId(roleId);
            PetGuardState state = stateOpt.orElseGet(() -> new PetGuardState(roleId, 0, 0L));
            ops.set(passLevelKey, state.getPassLevel());
            ops.set(fetchFlagKey, state.getFetchFlag());
            return state;
        }
        return new PetGuardState(roleId, passLevel, fetchFlag);
    }

    @Transactional
    public void saveState(PetGuardState state) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(PASS_LEVEL_KEY + state.getRoleId(), state.getPassLevel());
        ops.set(FETCH_FLAG_KEY + state.getRoleId(), state.getFetchFlag());
        petGuardRepository.save(state);
    }
}
