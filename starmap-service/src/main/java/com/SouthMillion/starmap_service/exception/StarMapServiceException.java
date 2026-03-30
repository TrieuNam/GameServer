package com.SouthMillion.starmap_service.exception;

public class StarMapServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public StarMapServiceException(String message) {
        super(message);
        this.errorCode = "STARMAP_ERROR";
    }
    
    public StarMapServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public StarMapServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "STARMAP_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
