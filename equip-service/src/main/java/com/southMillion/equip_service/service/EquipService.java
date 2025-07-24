package com.southMillion.equip_service.service;

import com.southMillion.equip_service.entity.EquipEntity;
import com.southMillion.equip_service.repository.EquipRepository;
import com.southMillion.equip_service.service.cache.EquipCacheService;
import com.southMillion.equip_service.service.producer.EquipEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class EquipService {
    private final EquipRepository repo;
    private final EquipCacheService cache;
    private final EquipEventProducer equipEventProducer;

    public EquipDto getEquip(String userId, int equipType) {
        EquipDto cached = cache.getEquipFromCache(userId, equipType);
        if (cached != null) return cached;

        Optional<EquipEntity> entity = repo.findByUserIdAndEquipType(userId, equipType);
        if (entity.isPresent()) {
            EquipDto dto = toDto(entity.get());
            cache.putEquipToCache(dto, 10);
            return dto;
        }
        return null;
    }

    public List<EquipDto> getAllEquip(String userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public EquipDto addOrUpdateEquip(EquipDto dto) {
        EquipEntity entity = repo.findByUserIdAndEquipType(dto.getUserId(), dto.getEquipType())
                .orElse(EquipEntity.builder().userId(dto.getUserId()).equipType(dto.getEquipType()).build());

        entity.setItemId(dto.getItemId());
        entity.setLevel(dto.getLevel());
        entity.setHp(dto.getHp());
        entity.setAttack(dto.getAttack());
        entity.setDefend(dto.getDefend());
        entity.setSpeed(dto.getSpeed());
        entity.setAttrType1(dto.getAttrType1());
        entity.setAttrValue1(dto.getAttrValue1());
        entity.setAttrType2(dto.getAttrType2());
        entity.setAttrValue2(dto.getAttrValue2());

        EquipEntity saved = repo.save(entity);
        EquipDto result = toDto(saved);
        cache.putEquipToCache(result, 10);

        // Gửi event Kafka khi update/thêm trang bị
        equipEventProducer.sendEquipEvent(result.getUserId(), "EQUIP_ADDED_OR_UPDATED", result);

        return result;
    }

    public boolean deleteEquip(String userId, int equipType) {
        Optional<EquipEntity> entityOpt = repo.findByUserIdAndEquipType(userId, equipType);
        if (entityOpt.isPresent()) {
            repo.delete(entityOpt.get());
            cache.removeEquipFromCache(userId, equipType);

            // Gửi event Kafka khi xóa trang bị
            equipEventProducer.sendEquipEvent(userId, "EQUIP_DELETED", new EquipDeletedPayload(equipType));
            return true;
        }
        return false;
    }

    public EquipDto toDto(EquipEntity e) {
        return EquipDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .equipType(e.getEquipType())
                .itemId(e.getItemId())
                .level(e.getLevel())
                .hp(e.getHp())
                .attack(e.getAttack())
                .defend(e.getDefend())
                .speed(e.getSpeed())
                .attrType1(e.getAttrType1())
                .attrValue1(e.getAttrValue1())
                .attrType2(e.getAttrType2())
                .attrValue2(e.getAttrValue2())
                .build();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EquipDeletedPayload {
        private int equipType;
    }
}