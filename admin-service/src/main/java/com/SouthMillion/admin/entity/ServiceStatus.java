package com.SouthMillion.admin.entity;

/**
 * Service Status Enum
 */
public enum ServiceStatus {
    /**
     * Service is not running
     */
    STOPPED,

    /**
     * Service is starting up
     */
    STARTING,

    /**
     * Service is running and healthy
     */
    RUNNING,

    /**
     * Service is shutting down
     */
    STOPPING,

    /**
     * Service encountered an error
     */
    ERROR,

    /**
     * Service status is unknown
     */
    UNKNOWN
}
