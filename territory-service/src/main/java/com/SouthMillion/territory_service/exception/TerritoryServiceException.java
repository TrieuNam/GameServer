package com.SouthMillion.territory_service.exception;

public class TerritoryServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public TerritoryServiceException(String message) {
        super(message);
        this.errorCode = "TERRITORY_ERROR";
    }
    
    public TerritoryServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TerritoryServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TERRITORY_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
