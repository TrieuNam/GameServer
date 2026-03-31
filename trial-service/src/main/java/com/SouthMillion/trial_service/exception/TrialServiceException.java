package com.SouthMillion.trial_service.exception;

public class TrialServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public TrialServiceException(String message) {
        super(message);
        this.errorCode = "TRIAL_ERROR";
    }
    
    public TrialServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public TrialServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TRIAL_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
