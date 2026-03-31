package com.SouthMillion.crafting_service.repository;

import com.SouthMillion.crafting_service.entity.CraftingRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CraftingRecipeRepository extends JpaRepository<CraftingRecipe, Long> {
    Optional<CraftingRecipe> findByRecipeId(Integer recipeId);
    List<CraftingRecipe> findByEnabledTrue();
    List<CraftingRecipe> findByRequiredLevelLessThanEqualAndEnabledTrue(Integer level);
}
