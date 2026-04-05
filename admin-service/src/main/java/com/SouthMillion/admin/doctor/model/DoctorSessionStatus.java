package com.SouthMillion.admin.doctor.model;

/**
 * Service Doctor session states for monitoring and approval flow.
 */
public enum DoctorSessionStatus {
    IDLE,
    WAITING,
    STARTED,
    ERROR,
    NEEDS_APPROVAL,
    APPROVED,
    REJECTED,
    FIXING,
    BUILDING,
    VERIFIED,
    FAILED
}
