package com.SouthMillion.report_service.enums;

public enum ReportEventType {
    APP_START("APP_START", 1),
    APP_CRASH("APP_CRASH", 2),
    USER_LOGIN("USER_LOGIN", 3),
    USER_LOGOUT("USER_LOGOUT", 4),
    PAYMENT_FAILED("PAYMENT_FAILED", 5),
    NETWORK_ERROR("NETWORK_ERROR", 6),
    DATA_SYNC_ERROR("DATA_SYNC_ERROR", 7),
    PERFORMANCE_ISSUE("PERFORMANCE_ISSUE", 8),
    APP_EXIT("APP_EXIT", 9),
    ERROR_REPORT("ERROR_REPORT", 10),
    CUSTOM("CUSTOM", 11);

    private final String name;
    private final int code;

    ReportEventType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ReportEventType fromCode(int code) {
        for (ReportEventType type : ReportEventType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
