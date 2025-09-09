package com.southMillion.webSocket_server.net;

public interface MsgIds {
    // ===== Login
    public static final int CS_LOGIN_REQ        = 7056; // PB_CSLoginToAccount
    public static final int SC_LOGIN_ACK        = 7000; // PB_SCLoginToAccount
    public static final int SC_ACCOUNT_KEY_ERR  = 7004; // PB_SCAccountKeyError

    // ===== Heartbeat / Time / Link
    public static final int CS_HEARTBEAT_REQ    = 1053; // PB_CSHeartbeatReq
    public static final int SC_HEARTBEAT_RESP   = 1003; // PB_SCHeartbeatResp

    public static final int CS_TIME_REQ         = 9050; // PB_CSTimeReq
    public static final int SC_TIME_ACK         = 9000; // PB_SCTimeAck
    public static final int SC_DISCONNECT_NOTICE= 9001; // PB_SCDisconnectNotice

    // ===== Role
    public static final int SC_ROLE_INFO_ACK    = 1400; // PB_SCRoleInfoAck
    public static final int SC_ROLE_ATTR_LIST   = 1401; // PB_SCRoleAttrList
    public static final int SC_ROLE_EXP_CHANGE  = 1402; // PB_SCRoleExpChange
    public static final int SC_ROLE_LEVEL_CHANGE= 1403; // PB_SCRoleLevelChange
    public static final int CS_ROLE_WXINFO_SET  = 1405; // PB_CSRoleWXInfoSetReq
}
