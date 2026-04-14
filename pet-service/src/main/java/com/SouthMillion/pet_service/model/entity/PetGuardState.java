package com.SouthMillion.pet_service.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_guard_state")
public class PetGuardState {
    @Id
    @NotNull(message = "roleId is required")
    @Min(value = 1, message = "roleId must be positive")
    private Long roleId;

    @NotNull(message = "passLevel is required")
    @Min(value = 0, message = "passLevel must be >= 0")
    private Integer passLevel;

    @NotNull(message = "fetchFlag is required")
    @Min(value = 0, message = "fetchFlag must be >= 0")
    private Long fetchFlag;
}
