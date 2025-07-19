package com.SouthMillion.battleserver_service.service.items.petWeapons;

import com.SouthMillion.battleserver_service.dto.items.petWeapons.PetWeaponItemDto;
import com.SouthMillion.battleserver_service.service.serviceClient.ConfigServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PetWeaponItemService {

    @Autowired
    private ConfigServiceClient configServiceClient; // Feign lấy pet_weapon_item.json từ config-service
    @Autowired
    private ObjectMapper objectMapper;

    public List<PetWeaponItemDto> getAllPetWeaponItems() {
        JsonNode json = configServiceClient.getPetWeaponItemConfigFile();
        JsonNode arr = json.get("pet_weapon");
        if (arr == null || !arr.isArray()) return Collections.emptyList();

        List<PetWeaponItemDto> list = new ArrayList<>();
        for (JsonNode node : arr) {
            try {
                list.add(objectMapper.treeToValue(node, PetWeaponItemDto.class));
            } catch (Exception e) {
                // log nếu cần
            }
        }
        return list;
    }

    public PetWeaponItemDto getPetWeaponItemById(String id) {
        JsonNode json = configServiceClient.getPetWeaponItemConfigFile();
        JsonNode arr = json.get("pet_weapon");
        if (arr == null || !arr.isArray()) return null;

        for (JsonNode node : arr) {
            if (id.equals(node.get("id").asText())) {
                try {
                    return objectMapper.treeToValue(node, PetWeaponItemDto.class);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}