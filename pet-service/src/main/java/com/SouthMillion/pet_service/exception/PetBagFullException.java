package com.SouthMillion.pet_service.exception;

/**
 * Exception thrown when pet bag is full
 */
public class PetBagFullException extends PetServiceException {
    public PetBagFullException(String userId, int currentSize, int maxSize) {
        super(String.format("Pet bag is full: userId=%d, current=%d, max=%d", 
            userId, currentSize, maxSize));
    }
}
