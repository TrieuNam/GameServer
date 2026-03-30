package com.SouthMillion.angel_service.exception;

/**
 * Angel Service Exception
 */
public class AngelServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public AngelServiceException(String message) {
        super(message);
        this.errorCode = "ANGEL_ERROR";
    }
    
    public AngelServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AngelServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ANGEL_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
