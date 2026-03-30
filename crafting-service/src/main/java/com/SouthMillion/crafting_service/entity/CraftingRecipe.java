package com.SouthMillion.crafting_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "crafting_recipe",
        indexes = {
            @Index(name = "idx_recipe_enabled", columnList = "enabled"),
            @Index(name = "idx_recipe_level", columnList = "requiredLevel")
        })
@Getter @Setter
public class CraftingRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false, unique = true)
    private Integer recipeId;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Column(name = "result_item_id", nullable = false)
    private Integer resultItemId;

    @Column(name = "result_amount", nullable = false)
    private Integer resultAmount;

    @Column(name = "materials_json", nullable = false, columnDefinition = "TEXT")
    private String materialsJson;

    @Column(name = "craft_time", nullable = false)
    private Long craftTime;

    @Column(name = "required_level", nullable = false)
    private Integer requiredLevel;

    @Column(name = "coin_cost")
    private Long coinCost;

    @Column(name = "enabled", columnDefinition = "TINYINT")
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
