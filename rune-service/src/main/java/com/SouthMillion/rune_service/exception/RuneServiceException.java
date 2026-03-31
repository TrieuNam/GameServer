package com.SouthMillion.rune_service.exception;

public class RuneServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public RuneServiceException(String message) {
        super(message);
        this.errorCode = "RUNE_ERROR";
    }
    
    public RuneServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RuneServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RUNE_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
