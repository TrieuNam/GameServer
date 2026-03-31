package com.SouthMillion.artifact_service.exception;

public class ArtifactServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public ArtifactServiceException(String message) {
        super(message);
        this.errorCode = "ARTIFACT_ERROR";
    }
    
    public ArtifactServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ArtifactServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ARTIFACT_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
