package com.SouthMillion.pet_service.exception;

/**
 * Exception thrown when pet level exceeds role level
 */
public class PetLevelExceedRoleLevelException extends PetServiceException {
    public PetLevelExceedRoleLevelException(Integer petLevel, Integer roleLevel) {
        super(String.format("Pet level (%d) cannot exceed role level (%d)", petLevel, roleLevel));
    }
}
