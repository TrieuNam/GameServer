package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.entity.PetRemainsEntity;
import com.SouthMillion.pet_service.repository.PetRemainsRepository;
import org.SouthMillion.dto.pet.PetRemainsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetRemainsService {
    @Autowired
    private PetRemainsRepository repo;

    // Lấy toàn bộ remains của user
    public List<PetRemainsDTO> getAllRemains(Long userId) {
        return repo.findByUserId(userId)
                .stream()
                .map(PetRemainsEntity::fromEntity)
                .collect(Collectors.toList());
    }

    // Lấy remains theo seq (nếu cần)
    public PetRemainsDTO getRemains(Long userId, Integer seq) {
        return repo.findByUserIdAndSeq(userId, seq)
                .stream()
                .findFirst()
                .map(PetRemainsEntity::fromEntity)
                .orElse(null);
    }

    // Thêm remains mới
    public PetRemainsDTO addRemains(Long userId, PetRemainsDTO dto) {
        PetRemainsEntity entity = PetRemainsEntity.toEntity(dto, userId);
        PetRemainsEntity saved = repo.save(entity);
        return PetRemainsEntity.fromEntity(saved);
    }

    // Cập nhật remains
    public PetRemainsDTO updateRemains(Long userId, PetRemainsDTO dto) {
        List<PetRemainsEntity> list = repo.findByUserIdAndSeq(userId, dto.getSeq());
        if (list.isEmpty()) throw new RuntimeException("Remains not found");
        PetRemainsEntity entity = list.get(0);
        entity.setGrade(dto.getGrade());
        entity.setLevel(dto.getLevel());
        entity.setExp(dto.getExp());
        entity.setClothId(dto.getClothId());
        entity.setIndex(dto.getIndex());
        PetRemainsEntity saved = repo.save(entity);
        return PetRemainsEntity.fromEntity(saved);
    }
}