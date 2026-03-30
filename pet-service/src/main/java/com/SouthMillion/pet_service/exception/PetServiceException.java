package com.SouthMillion.pet_service.exception;

/**
 * Base exception for pet service
 */
public class PetServiceException extends RuntimeException {
    public PetServiceException(String message) {
        super(message);
    }

    public PetServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
