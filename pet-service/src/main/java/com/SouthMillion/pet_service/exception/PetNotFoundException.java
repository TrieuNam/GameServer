package com.SouthMillion.pet_service.exception;

/**
 * Exception thrown when pet is not found
 */
public class PetNotFoundException extends PetServiceException {
    public PetNotFoundException(String userId, Integer petIndex) {
        super(String.format("Pet not found: userId=%d, petIndex=%d", userId, petIndex));
    }
}
