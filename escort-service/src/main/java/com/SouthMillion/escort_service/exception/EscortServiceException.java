package com.SouthMillion.escort_service.exception;

public class EscortServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public EscortServiceException(String message) {
        super(message);
        this.errorCode = "ESCORT_ERROR";
    }
    
    public EscortServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public EscortServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ESCORT_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
