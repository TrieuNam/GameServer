package com.SouthMillion.battleserver_service.controller.items.pets;

import com.SouthMillion.battleserver_service.dto.items.pets.PetItemDto;
import com.SouthMillion.battleserver_service.service.items.pets.PetItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/pet_item")
public class PetItemController {

    @Autowired
    private PetItemService petItemService;

    @GetMapping
    public List<PetItemDto> getAllPetItems() {
        return petItemService.getAllPetItems();
    }

    @GetMapping("/{id}")
    public PetItemDto getPetItemById(@PathVariable String id) {
        return petItemService.getPetItemById(id);
    }
}