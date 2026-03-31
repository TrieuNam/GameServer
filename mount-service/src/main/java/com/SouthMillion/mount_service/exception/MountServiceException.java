package com.SouthMillion.mount_service.exception;

/**
 * Mount Service Exception
 * Used for all mount-related business logic errors
 */
public class MountServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public MountServiceException(String message) {
        super(message);
        this.errorCode = "MOUNT_ERROR";
    }
    
    public MountServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public MountServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MOUNT_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
